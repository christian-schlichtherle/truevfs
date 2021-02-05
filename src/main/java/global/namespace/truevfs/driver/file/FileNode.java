/*
 * Copyright © 2005 - 2021 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package global.namespace.truevfs.driver.file;

import global.namespace.truevfs.commons.cio.Entry;
import global.namespace.truevfs.commons.cio.InputSocket;
import global.namespace.truevfs.commons.cio.IoBuffer;
import global.namespace.truevfs.commons.cio.OutputSocket;
import global.namespace.truevfs.commons.shed.BitField;
import global.namespace.truevfs.kernel.api.FsAbstractNode;
import global.namespace.truevfs.kernel.api.FsAccessOption;
import global.namespace.truevfs.kernel.api.FsNode;
import global.namespace.truevfs.kernel.api.FsNodeName;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.PosixFilePermission;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;

import static global.namespace.truevfs.kernel.api.FsAccessOptions.NONE;
import static global.namespace.truevfs.kernel.api.FsNodeName.SEPARATOR_CHAR;
import static java.io.File.separatorChar;
import static java.nio.file.Files.*;
import static java.nio.file.attribute.PosixFilePermission.*;

/**
 * Adapts a {@link Path} instance to a {@link FsNode}.
 *
 * @author Christian Schlichtherle
 */
@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
class FileNode extends FsAbstractNode implements IoBuffer {

    private static final Path CURRENT_DIRECTORY = Paths.get(".");

    private final Path path;
    private final String name;

    volatile @CheckForNull FileBufferPool pool;

    FileNode(final Path path) {
        assert null != path;
        this.path = path;
        this.name = path.toString(); // deliberately breaks contract for FsNode.getName()
    }

    FileNode(final Path path, final FsNodeName name) {
        assert null != path;
        this.path = path.resolve(name.getPath());
        this.name = name.toString();
    }

    private BasicFileAttributes readBasicFileAttributes() throws IOException {
        return readAttributes(path, BasicFileAttributes.class);
    }

    final FileNode createIoBuffer() throws IOException {
        FileBufferPool pool = this.pool;
        if (null == pool) {
            this.pool = pool = new FileBufferPool(getParent(), getFileName());
        }
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

    @Override
    public void release() throws IOException {
    }

    /**
     * Returns the decorated file.
     */
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
            if (attr.isRegularFile()) return FILE_TYPE;
            else if (attr.isDirectory()) return DIRECTORY_TYPE;
            else if (attr.isSymbolicLink()) return SYMLINK_TYPE;
            else if (attr.isOther()) return SPECIAL_TYPE;
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
    public Optional<Boolean> isPermitted(final Access type, final Entity entity) {
        if (entity instanceof PosixEntity) {
            try {
                final Set<PosixFilePermission> permissions = getPosixFilePermissions(path);
                switch ((PosixEntity) entity) {
                    case USER:
                        switch (type) {
                            case READ:
                                return Optional.of(permissions.contains(OWNER_READ));
                            case WRITE:
                                return Optional.of(permissions.contains(OWNER_WRITE));
                            case EXECUTE:
                                return Optional.of(permissions.contains(OWNER_EXECUTE));
                        }
                        break;
                    case GROUP:
                        switch (type) {
                            case READ:
                                return Optional.of(permissions.contains(GROUP_READ));
                            case WRITE:
                                return Optional.of(permissions.contains(GROUP_WRITE));
                            case EXECUTE:
                                return Optional.of(permissions.contains(GROUP_EXECUTE));
                        }
                        break;
                    case OTHER:
                        switch (type) {
                            case READ:
                                return Optional.of(permissions.contains(OTHERS_READ));
                            case WRITE:
                                return Optional.of(permissions.contains(OTHERS_WRITE));
                            case EXECUTE:
                                return Optional.of(permissions.contains(OTHERS_EXECUTE));
                        }
                }
            } catch (UnsupportedOperationException | IOException ignore) {
                // Unsupported, doesn't exist or inaccessible. In either case...
            }
        }
        return Optional.empty();
    }

    @Override
    public final @Nullable
    Set<String> getMembers() {
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
    public final InputSocket<FileNode> input() {
        return input(NONE);
    }

    final InputSocket<FileNode> input(BitField<FsAccessOption> options) {
        return new FileInputSocket(options, this);
    }

    @Override
    public final OutputSocket<FileNode> output() {
        return output(NONE, Optional.empty());
    }

    final OutputSocket<FileNode> output(BitField<FsAccessOption> options, Optional<? extends Entry> template) {
        return new FileOutputSocket(options, this, template);
    }
}
