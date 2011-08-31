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

import de.schlichtherle.truezip.entry.Entry;
import static de.schlichtherle.truezip.entry.Entry.*;
import static de.schlichtherle.truezip.entry.Entry.Access.*;
import de.schlichtherle.truezip.fs.FsOutputOption;
import static de.schlichtherle.truezip.fs.FsOutputOption.*;
import de.schlichtherle.truezip.io.DecoratingOutputStream;
import de.schlichtherle.truezip.io.DecoratingSeekableByteChannel;
import de.schlichtherle.truezip.socket.IOSocket;
import de.schlichtherle.truezip.socket.OutputSocket;
import de.schlichtherle.truezip.util.BitField;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.DefaultAnnotation;
import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import java.io.IOException;
import java.io.OutputStream;
import static java.lang.Boolean.*;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.AccessMode;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import static java.nio.file.Files.*;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import static java.nio.file.StandardCopyOption.*;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.FileTime;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * An output socket for a file entry.
 *
 * @since   TrueZIP 7.2
 * @see     FileInputSocket
 * @author  Christian Schlichtherle
 * @version $Id$
 */
@DefaultAnnotation(NonNull.class)
final class FileOutputSocket extends OutputSocket<FileEntry> {

    private static final int
            INITIAL_CAPACITY = FsOutputOption.values().length * 4 / 3;
    private static final StandardOpenOption[] 
            WRITE_STANDARD_OPEN_OPTION = {
                StandardOpenOption.WRITE,
                StandardOpenOption.TRUNCATE_EXISTING,
                StandardOpenOption.CREATE,
            };

    private final               FileEntry                entry;
    private final               BitField<FsOutputOption> options;
    private final @CheckForNull Entry                    template;

    FileOutputSocket(   final               FileEntry                entry,
                        final               BitField<FsOutputOption> options,
                        final @CheckForNull Entry                    template) {
        assert null != entry;
        assert null != options;
        if (options.get(EXCLUSIVE) && options.get(APPEND))
            throw new IllegalArgumentException();
        this.entry    = entry;
        this.options  = options;
        this.template = template;
    }

    @Override
    public FileEntry getLocalTarget() {
        return entry;
    }

    private FileEntry begin() throws IOException {
        final FileEntry temp;
        final Path entryFile = entry.getPath();
        Boolean exists = null;
        if (options.get(EXCLUSIVE) && (exists = exists(entryFile)))
            throw new FileAlreadyExistsException(entry.toString());
        if (options.get(CACHE)) {
            if (TRUE.equals(exists)
                    || null == exists && (exists = exists(entryFile)))
                entryFile   .getFileSystem()
                            .provider()
                            .checkAccess(entryFile, AccessMode.WRITE);
            temp = entry.createTempFile();
        } else {
            temp = entry;
        }
        if (options.get(CREATE_PARENTS) && !TRUE.equals(exists)) {
            final Path parentFile = entryFile.getParent();
            if (null != parentFile)
                createDirectories(parentFile);
        }
        return temp;
    }

    private void append(final FileEntry temp) throws IOException {
        if (temp != entry && options.get(APPEND) && exists(entry.getPath()))
            IOSocket.copy(entry.getInputSocket(), temp.getOutputSocket());
    }

    private Set<OpenOption> optionSet() {
        final Set<OpenOption> set = new HashSet<OpenOption>(INITIAL_CAPACITY);
        Collections.addAll(set, WRITE_STANDARD_OPEN_OPTION);
        if (options.get(APPEND)) {
            set.add(StandardOpenOption.APPEND);
            set.remove(StandardOpenOption.TRUNCATE_EXISTING);
        }
        if (options.get(EXCLUSIVE))
            set.add(StandardOpenOption.CREATE_NEW);
        return set;
    }

    private OpenOption[] optionArray() {
        final Set<OpenOption> set = optionSet();
        return set.toArray(new OpenOption[set.size()]);
    }

    private void commit(final FileEntry temp) throws IOException {
        final Path entryFile = entry.getPath();
        final Path tempFile = temp.getPath();
        if (temp != entry) {
            copyAttributes(tempFile);
            try {
                move(tempFile, entryFile, REPLACE_EXISTING);
            } catch (IOException ex) {
                // Slow.
                /*Files.copy(tempFile, entryFile,
                        StandardCopyOption.REPLACE_EXISTING);*/
                // Fast.
                IOSocket.copy(  temp.getInputSocket(),
                                entry.getOutputSocket());
                copyAttributes(entryFile);
            }
            release(temp, null);
        } else {
            copyAttributes(entryFile);
        }
    }

    private void copyAttributes(final Path file) throws IOException {
        final Entry template = this.template;
        if (null == template)
            return;
        getFileAttributeView(file, BasicFileAttributeView.class)
                .setTimes(  toFileTime(template.getTime(WRITE)),
                            toFileTime(template.getTime(READ)),
                            toFileTime(template.getTime(CREATE)));
    }

    private static @Nullable FileTime toFileTime(long time) {
        return UNKNOWN == time ? null : FileTime.fromMillis(time);
    }

    private void release(
            final FileEntry temp,
            final @CheckForNull IOException ex)
    throws IOException {
        try {
            temp.release();
        } catch (IOException ex2) {
            ex2.initCause(ex);
            throw ex2;
        }
    }

    @Override
    public SeekableByteChannel newSeekableByteChannel() throws IOException {
        final FileEntry temp = begin();

        class SeekableByteChannel extends DecoratingSeekableByteChannel {
            boolean closed;

            SeekableByteChannel() throws IOException {
                super(Files.newByteChannel(temp.getPath(), optionSet()));
            }

            @Override
            public void close() throws IOException {
                if (closed)
                    return;
                closed = true;
                try {
                    delegate.close();
                } finally {
                    commit(temp);
                }
            }
        } // SeekableByteChannel

        try {
            append(temp);
            return new SeekableByteChannel();
        } catch (IOException ex) {
            release(temp, ex);
            throw ex;
        }
    }

    @Override
    public OutputStream newOutputStream() throws IOException {
        final FileEntry temp = begin();

        class OutputStream extends DecoratingOutputStream {
            boolean closed;

            OutputStream() throws IOException {
                super(Files.newOutputStream(temp.getPath(), optionArray()));
            }

            @Override
            public void close() throws IOException {
                if (closed)
                    return;
                closed = true;
                try {
                    delegate.close();
                } finally {
                    commit(temp);
                }
            }
        } // OutputStream

        try {
            append(temp);
            return new OutputStream();
        } catch (IOException ex) {
            release(temp, ex);
            throw ex;
        }
    }
}
