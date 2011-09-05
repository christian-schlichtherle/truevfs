/*
 * Copyright (C) 2011 Schlichtherle IT Services
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package de.schlichtherle.truezip.fs.nio.file;

import de.schlichtherle.truezip.entry.Entry;
import de.schlichtherle.truezip.fs.FsEntry;
import de.schlichtherle.truezip.fs.FsEntryName;
import static de.schlichtherle.truezip.fs.FsEntryName.*;
import de.schlichtherle.truezip.fs.FsOutputOption;
import static de.schlichtherle.truezip.fs.FsOutputOptions.*;
import de.schlichtherle.truezip.socket.IOEntry;
import de.schlichtherle.truezip.socket.InputSocket;
import de.schlichtherle.truezip.socket.OutputSocket;
import de.schlichtherle.truezip.util.BitField;
import de.schlichtherle.truezip.util.Pool.Releasable;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.DefaultAnnotation;
import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import static java.io.File.*;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import static java.nio.file.Files.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import net.jcip.annotations.Immutable;

/**
 * Adapts a {@link Path} instance to a {@link FsEntry}.
 *
 * @since   TrueZIP 7.2
 * @author  Christian Schlichtherle
 * @version $Id$
 */
@Immutable
@DefaultAnnotation(NonNull.class)
@edu.umd.cs.findbugs.annotations.SuppressWarnings("JCIP_FIELD_ISNT_FINAL_IN_IMMUTABLE_CLASS")
class FileEntry
extends FsEntry
implements IOEntry<FileEntry>, Releasable<IOException> {

    private static final Path CURRENT_DIRECTORY = Paths.get(".");

    private final Path path;
    private final String name;
    volatile @CheckForNull TempFilePool pool;

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
        TempFilePool pool = this.pool;
        if (null == pool) {
            final Path dir = getTempDir(path);
            final String name = getTempName();
            this.pool = pool = new TempFilePool(dir, name);
        }
        return pool.allocate();
    }

    private Path getTempDir(Path path) {
        /*for (; null != path; path = path.getParent())
            if (isDirectory(path))
                return path;*/
        path = path.getParent();
        if (null != path)
            return path;
        return CURRENT_DIRECTORY;
    }

    private @Nullable String getTempName() {
        // See http://java.net/jira/browse/TRUEZIP-152
        final Path path = this.path.getFileName();
        return path == null ? null : path.toString();
    }

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
    @SuppressWarnings("unchecked")
    public final Set<Type> getTypes() {
        try {
            final BasicFileAttributes attr = readBasicFileAttributes();
            if (attr.isRegularFile())
                return FILE_TYPE_SET;
            else if (attr.isDirectory())
                return DIRECTORY_TYPE_SET;
            else if (attr.isSymbolicLink())
                return SYMLINK_TYPE_SET;
            else if (attr.isOther())
                return SPECIAL_TYPE_SET;
        } catch (IOException ignore) {
            // This doesn't exist or may be inaccessible. In either case...
        }
        return Collections.EMPTY_SET;
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
                case WRITE:
                    return attr.lastModifiedTime().toMillis();
                case READ:
                    return attr.lastAccessTime().toMillis();
                case CREATE:
                    return attr.creationTime().toMillis();
            }
        } catch (IOException ignore) {
            // This doesn't exist or may be inaccessible. In either case...
        }
        return UNKNOWN;
    }

    @Override
    public final @Nullable Set<String> getMembers() {
        try {
            final DirectoryStream<Path> stream = newDirectoryStream(path);
            try {
                final Set<String> result = new LinkedHashSet<String>();
                for (final Path member : stream)
                    result.add(member.getFileName().toString());
                return result;
            } finally {
                stream.close();
            }
        } catch (IOException ignore) {
            // This isn't a directory or may be inaccessible. In either case...
            return null;
        }
    }

    @Override
    public final InputSocket<FileEntry> getInputSocket() {
        return new FileInputSocket(this);
    }

    @Override
    public final OutputSocket<FileEntry> getOutputSocket() {
        return new FileOutputSocket(this, NO_OUTPUT_OPTIONS, null);
    }

    final OutputSocket<FileEntry> getOutputSocket(
            BitField<FsOutputOption> options,
            @CheckForNull Entry template) {
        return new FileOutputSocket(this, options, template);
    }
}
