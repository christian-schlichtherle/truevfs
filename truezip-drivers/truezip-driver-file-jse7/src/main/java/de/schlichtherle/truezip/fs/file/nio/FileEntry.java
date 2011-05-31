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
package de.schlichtherle.truezip.fs.file.nio;

import de.schlichtherle.truezip.socket.IOPool.Entry;
import de.schlichtherle.truezip.util.BitField;
import de.schlichtherle.truezip.fs.FsOutputOption;
import static de.schlichtherle.truezip.fs.FsOutputOptions.*;
import de.schlichtherle.truezip.socket.InputSocket;
import de.schlichtherle.truezip.socket.OutputSocket;
import static de.schlichtherle.truezip.entry.Entry.Size.*;
import de.schlichtherle.truezip.fs.FsEntry;
import de.schlichtherle.truezip.fs.FsEntryName;
import de.schlichtherle.truezip.socket.IOPool;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.DefaultAnnotation;
import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import java.io.IOException;
import java.util.Collections;
import java.lang.reflect.UndeclaredThrowableException;
import java.nio.file.DirectoryStream;
import static java.nio.file.Files.*;
import java.nio.file.Path;
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
implements IOPool<FileEntry>, Entry<FileEntry> {

    private final Path path;
    private final String name;
    volatile @CheckForNull TempFilePool pool;

    FileEntry(final Path path) {
        assert null != path;
        this.path = path;
        this.name = path.getFileName().toString();
    }

    FileEntry(final Path path, final FsEntryName name) {
        assert null != path;
        this.path = path.resolve(name.getPath());
        this.name = name.toString();
    }

    @Override
    public FileEntry allocate() throws IOException {
        TempFilePool pool = this.pool;
        if (null == pool)
            pool = this.pool = new TempFilePool(path.getParent());
        return pool.allocate();
    }

    @Override
    public void release(Entry<FileEntry> resource) throws IOException {
        resource.release();
    }

    @Override
    public void release() throws IOException {
        throw new UnsupportedOperationException();
    }

    /** Returns the decorated file. */
    final Path getPath() {
        return path;
    }

    @Override
    public final String getName() {
        return name;
    }

    @Override
    @SuppressWarnings("unchecked")
    public final Set<Type> getTypes() {
        if (isRegularFile(path))
            return FILE_TYPE_SET;
        else if (isDirectory(path))
            return DIRECTORY_TYPE_SET;
        else if (exists(path))
            return SPECIAL_TYPE_SET;
        else
            return Collections.EMPTY_SET;
    }

    @Override
    public final boolean isType(final Type type) {
        switch (type) {
        case FILE:
            return isRegularFile(path);
        case DIRECTORY:
            return isDirectory(path);
        case SPECIAL:
            return exists(path) && !isRegularFile(path) && !isDirectory(path);
        default:
            return false;
        }
    }

    @Override
    public final long getSize(final Size type) {
        try {
            return (DATA == type || STORAGE == type) && exists(path)
                    ? size(path)
                    : UNKNOWN;
        } catch (IOException ex) {
            throw new UndeclaredThrowableException(ex);
        }
    }

    @Override
    public final long getTime(Access type) {
        try {
            return WRITE == type && exists(path)
                    ? getLastModifiedTime(path).toMillis()
                    : UNKNOWN;
        } catch (IOException ex) {
            throw new UndeclaredThrowableException(ex);
        }
    }

    @Override
    public final @Nullable Set<String> getMembers() {
        if (!isDirectory(path))
            return null;
        final Set<String> result = new LinkedHashSet<String>();
        try {
            final DirectoryStream<Path> stream = newDirectoryStream(path);
            try {
                for (final Path member : stream)
                    result.add(member.getFileName().toString());
            } finally {
                stream.close();
            }
        } catch (IOException ex) {
            throw new UndeclaredThrowableException(ex);
        }
        return result;
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
