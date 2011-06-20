/*
 * Copyright (C) 2011 Schlichtherle IT Services
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.schlichtherle.truezip.fs.nio.file;

import de.schlichtherle.truezip.socket.IOEntry;
import de.schlichtherle.truezip.util.Pool.Releasable;
import de.schlichtherle.truezip.util.BitField;
import de.schlichtherle.truezip.fs.FsOutputOption;
import static de.schlichtherle.truezip.fs.FsOutputOptions.*;
import de.schlichtherle.truezip.socket.InputSocket;
import de.schlichtherle.truezip.socket.OutputSocket;
import de.schlichtherle.truezip.fs.FsEntry;
import de.schlichtherle.truezip.fs.FsEntryName;
import static de.schlichtherle.truezip.fs.FsEntryName.*;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.DefaultAnnotation;
import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import java.io.IOException;
import java.util.Collections;
import java.nio.file.DirectoryStream;
import static java.nio.file.Files.*;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import static java.io.File.*;
import java.util.LinkedHashSet;
import java.util.Set;
import net.jcip.annotations.Immutable;

import static de.schlichtherle.truezip.entry.Entry.Type.*;
import static de.schlichtherle.truezip.entry.Entry.Access.*;

/**
 * Adapts a {@link Path} instance to a {@link FsEntry}.
 *
 * @author Christian Schlichtherle
 * @version $Id$
 */
@Immutable
@DefaultAnnotation(NonNull.class)
@edu.umd.cs.findbugs.annotations.SuppressWarnings("JCIP_FIELD_ISNT_FINAL_IN_IMMUTABLE_CLASS")
class FileEntry
extends FsEntry
implements IOEntry<FileEntry>, Releasable<IOException> {

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

    public final FileEntry createTempFile() throws IOException {
        TempFilePool pool = this.pool;
        if (null == pool)
            pool = this.pool = new TempFilePool(path.getParent());
        return pool.allocate();
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
        return new FileOutputSocket(this, NO_OUTPUT_OPTION, null);
    }

    final OutputSocket<FileEntry> getOutputSocket(
            BitField<FsOutputOption> options,
            @CheckForNull de.schlichtherle.truezip.entry.Entry template) {
        return new FileOutputSocket(this, options, template);
    }
}
