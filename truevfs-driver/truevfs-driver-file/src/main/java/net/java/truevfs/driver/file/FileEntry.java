/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.driver.file;

import static java.io.File.separatorChar;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import static java.nio.file.Files.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.PosixFilePermission;
import static java.nio.file.attribute.PosixFilePermission.*;
import java.util.LinkedHashSet;
import java.util.Set;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import net.java.truecommons.shed.BitField;
import net.java.truevfs.kernel.spec.FsAccessOption;
import net.java.truevfs.kernel.spec.FsAccessOptions;
import net.java.truevfs.kernel.spec.FsEntry;
import net.java.truevfs.kernel.spec.FsEntryName;
import static net.java.truevfs.kernel.spec.FsEntryName.SEPARATOR_CHAR;
import net.java.truevfs.kernel.spec.cio.Entry;
import static net.java.truevfs.kernel.spec.cio.Entry.PosixEntity.*;
import net.java.truevfs.kernel.spec.cio.InputSocket;
import net.java.truevfs.kernel.spec.cio.IoBuffer;
import net.java.truevfs.kernel.spec.cio.OutputSocket;

/**
 * Adapts a {@link Path} instance to a {@link FsEntry}.
 *
 * @author Christian Schlichtherle
 */
@Immutable
class FileEntry extends FsEntry implements IoBuffer<FileEntry> {

    private static final Path CURRENT_DIRECTORY = Paths.get(".");

    private final Path path;
    private final String name;

    @edu.umd.cs.findbugs.annotations.SuppressWarnings("JCIP_FIELD_ISNT_FINAL_IN_IMMUTABLE_CLASS")
    volatile @CheckForNull FileIoBufferPool pool;

    FileEntry(final Path path) {
        assert null != path;
        this.path = path;
        this.name = path.toString(); // deliberately breaks contract for FsEntry.getName()
    }

    FileEntry(final Path path, final FsEntryName name) {
        assert null != path;
        this.path = path.resolve(name.getPath());
        this.name = name.toString();
    }

    private BasicFileAttributes readBasicFileAttributes() throws IOException {
        return readAttributes(path, BasicFileAttributes.class);
    }

    final FileEntry createTempFile() throws IOException {
        FileIoBufferPool pool = this.pool;
        if (null == pool)
            this.pool = pool = new FileIoBufferPool(getParent(), getFileName());
        return pool.allocate();
    }

    private Path getParent() {
        final Path path = this.path.getParent();
        return null != path ? path : CURRENT_DIRECTORY;
    }

    private String getFileName() {
        // See http://java.net/jira/browse/TRUEZIP-152
        final Path path = this.path.getFileName();
        return null != path ? path.toString() : "";
    }

    /**
     * @throws UnsupportedOperationException always
     */
    @Override
    public void release() throws IOException {
    }

    /** Returns the decorated file. */
    final Path getPath() {
        return path;
    }

    @Override
    public final String getName() {
        return name.replace(separatorChar, SEPARATOR_CHAR); // postfix
    }

    @Override
    public final BitField<Type> getTypes() {
        try {
            final BasicFileAttributes attr = readBasicFileAttributes();
            if (attr.isRegularFile())
                return FILE_TYPE;
            else if (attr.isDirectory())
                return DIRECTORY_TYPE;
            else if (attr.isSymbolicLink())
                return SYMLINK_TYPE;
            else if (attr.isOther())
                return SPECIAL_TYPE;
        } catch (IOException ignore) {
            // This doesn't exist or may be inaccessible. In either case...
        }
        return NO_TYPES;
    }

    @Override
    public final boolean isType(final Type type) {
        try {
            switch (type) {
            case FILE:
                return readBasicFileAttributes().isRegularFile();
            case DIRECTORY:
                return readBasicFileAttributes().isDirectory();
            case SYMLINK:
                return readBasicFileAttributes().isSymbolicLink();
            case SPECIAL:
                return readBasicFileAttributes().isOther();
            }
        } catch (IOException ignored) {
        }
        return false;
    }

    @Override
    public final long getSize(final Size type) {
        try {
            return readBasicFileAttributes().size();
        } catch (IOException ignore) {
            // This doesn't exist or may be inaccessible. In either case...
            return UNKNOWN;
        }
    }

    @Override
    public final long getTime(Access type) {
        try {
            final BasicFileAttributes attr = readBasicFileAttributes();
            switch (type) {
                case CREATE:
                    return attr.creationTime().toMillis();
                case READ:
                    return attr.lastAccessTime().toMillis();
                case WRITE:
                    return attr.lastModifiedTime().toMillis();
            }
        } catch (IOException ignore) {
            // This doesn't exist or may be inaccessible. In either case...
        }
        return UNKNOWN;
    }

    @Override
    public Boolean isPermitted(final Access type, final Entity entity) {
        if (!(entity instanceof PosixEntity))
            return null;
        try {
            final Set<PosixFilePermission> permissions = getPosixFilePermissions(path);
            switch ((PosixEntity) entity) {
            case USER:
                switch (type) {
                    case READ:
                        return permissions.contains(OWNER_READ);
                    case WRITE:
                        return permissions.contains(OWNER_WRITE);
                    case EXECUTE:
                        return permissions.contains(OWNER_EXECUTE);
                }
                break;
            case GROUP:
                switch (type) {
                    case READ:
                        return permissions.contains(GROUP_READ);
                    case WRITE:
                        return permissions.contains(GROUP_WRITE);
                    case EXECUTE:
                        return permissions.contains(GROUP_EXECUTE);
                }
                break;
            case OTHER:
                switch (type) {
                    case READ:
                        return permissions.contains(OTHERS_READ);
                    case WRITE:
                        return permissions.contains(OTHERS_WRITE);
                    case EXECUTE:
                        return permissions.contains(OTHERS_EXECUTE);
                }
            }
        } catch (UnsupportedOperationException | IOException ignore) {
            // Unsupported, doesn't exist or inaccessible. In either case...
        }
        return null;
    }

    @Override
    public final @Nullable Set<String> getMembers() {
        try {
            try (final DirectoryStream<Path> stream = newDirectoryStream(path)) {
                final Set<String> result = new LinkedHashSet<>();
                for (final Path member : stream)
                    result.add(member.getFileName().toString());
                return result;
            }
        } catch (IOException ignore) {
            // This isn't a directory or may be inaccessible. In either case...
            return null;
        }
    }

    @Override
    public final InputSocket<FileEntry> input() {
        return new FileInputSocket(this);
    }

    @Override
    public final OutputSocket<FileEntry> output() {
        return new FileOutputSocket(FsAccessOptions.NONE, this, null);
    }

    final OutputSocket<FileEntry> output(
            BitField<FsAccessOption> options,
            @CheckForNull Entry template) {
        return new FileOutputSocket(options, this, template);
    }
}
