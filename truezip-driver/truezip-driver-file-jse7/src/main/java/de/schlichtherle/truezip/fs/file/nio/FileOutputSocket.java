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

import java.nio.file.OpenOption;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import edu.umd.cs.findbugs.annotations.DefaultAnnotation;
import de.schlichtherle.truezip.entry.Entry;
import de.schlichtherle.truezip.io.DecoratingOutputStream;
import de.schlichtherle.truezip.fs.FsOutputOption;
import de.schlichtherle.truezip.io.DecoratingSeekableByteChannel;
import de.schlichtherle.truezip.socket.IOSocket;
import de.schlichtherle.truezip.socket.OutputSocket;
import de.schlichtherle.truezip.util.BitField;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.NonNull;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import static java.nio.file.Files.*;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.FileTime;
import net.jcip.annotations.ThreadSafe;

import static de.schlichtherle.truezip.fs.FsOutputOption.*;
import static de.schlichtherle.truezip.entry.Entry.Access.*;
import static de.schlichtherle.truezip.entry.Entry.*;

/**
 * An output socket for a file entry.
 *
 * @see     FileInputSocket
 * @author  Christian Schlichtherle
 * @version $Id$
 */
@ThreadSafe
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
        this.entry    = entry;
        this.options  = options;
        this.template = template;
    }

    @Override
    public FileEntry getLocalTarget() {
        return entry;
    }

    @Override
    public SeekableByteChannel newSeekableByteChannel() throws IOException {
        final Path entryFile = entry.getPath();
        if (options.get(CREATE_PARENTS))
            createDirectories(entryFile.getParent());
        FileAlreadyExistsException exists = null;
        if (options.get(CACHE)) {
            try {
                createFile(entryFile);
                if (options.get(EXCLUSIVE))
                    throw new IOException(entryFile + " (file exists already)"); // this is obviously not atomic
            } catch (FileAlreadyExistsException ex) {
                exists = ex;
            }
        }
        final FileEntry temp = null != exists
                ? entry.createTempFile()
                : entry;
        final Path tempFile = temp.getPath();

        final Set<OpenOption> set = new HashSet<OpenOption>(INITIAL_CAPACITY);
        Collections.addAll(set, WRITE_STANDARD_OPEN_OPTION);
        if (options.get(APPEND))
            set.add(StandardOpenOption.APPEND);
        if (options.get(EXCLUSIVE))
            set.add(StandardOpenOption.CREATE_NEW);

        class SeekableByteChannel extends DecoratingSeekableByteChannel {
            boolean closed;

            SeekableByteChannel() throws IOException {
                super(newByteChannel(tempFile, set));
            }

            @Override
            public void close() throws IOException {
                if (closed)
                    return;
                closed = true;
                try {
                    delegate.close();
                } finally {
                    IOException ex = null;
                    try {
                        if (temp != entry) {
                            try {
                                try {
                                    move(tempFile, entryFile,
                                            StandardCopyOption.REPLACE_EXISTING);
                                } catch (IOException ex2) {
                                    Files.copy(tempFile, entryFile,
                                            StandardCopyOption.REPLACE_EXISTING);
                                }
                            } catch (IOException ex2) {
                                throw ex = ex2;
                            } finally {
                                try {
                                    temp.release();
                                } catch (IOException ex2) {
                                    throw (IOException) ex2.initCause(ex);
                                }
                            }
                        }
                    } finally {
                        final Entry template = FileOutputSocket.this.template;
                        if (null != template) {
                            try {
                                getFileAttributeView(entryFile, BasicFileAttributeView.class)
                                        .setTimes(  getFileTime(template, WRITE),
                                                    getFileTime(template, READ),
                                                    getFileTime(template, CREATE));
                            } catch (IOException ex2) {
                                throw (IOException) ex2.initCause(ex);
                            }
                        }
                    }
                }
            }
        } // class OutputStream

        try {
            if (temp != entry && options.get(APPEND))
                IOSocket.copy(  entry.getInputSocket(),
                                temp.getOutputSocket());
            return new SeekableByteChannel();
        } catch (IOException cause) {
            if (temp != entry) {
                try {
                    temp.release();
                } catch (IOException ex) {
                    throw (IOException) ex.initCause(cause);
                }
            }
            throw cause;
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    @edu.umd.cs.findbugs.annotations.SuppressWarnings("RV_RETURN_VALUE_IGNORED_BAD_PRACTICE")
    public OutputStream newOutputStream() throws IOException {
        final Path entryFile = entry.getPath();
        if (options.get(CREATE_PARENTS))
            createDirectories(entryFile.getParent());
        FileAlreadyExistsException exists = null;
        if (options.get(CACHE)) {
            try {
                createFile(entryFile);
                if (options.get(EXCLUSIVE))
                    throw new IOException(entryFile + " (file exists already)"); // this is obviously not atomic
            } catch (FileAlreadyExistsException ex) {
                exists = ex;
            }
        }
        final FileEntry temp = null != exists
                ? entry.createTempFile()
                : entry;
        final Path tempFile = temp.getPath();

        final Set<OpenOption> set = new HashSet<OpenOption>(INITIAL_CAPACITY);
        Collections.addAll(set, WRITE_STANDARD_OPEN_OPTION);
        if (options.get(APPEND))
            set.add(StandardOpenOption.APPEND);
        if (options.get(EXCLUSIVE))
            set.add(StandardOpenOption.CREATE_NEW);

        class OutputStream extends DecoratingOutputStream {
            boolean closed;

            OutputStream() throws IOException {
                super(newOutputStream(tempFile,
                        set.toArray(new StandardOpenOption[set.size()])));
            }

            @Override
            public void close() throws IOException {
                if (closed)
                    return;
                closed = true;
                try {
                    delegate.close();
                } finally {
                    IOException ex = null;
                    try {
                        if (temp != entry) {
                            try {
                                try {
                                    move(tempFile, entryFile,
                                            StandardCopyOption.REPLACE_EXISTING);
                                } catch (IOException ex2) {
                                    Files.copy(tempFile, entryFile,
                                            StandardCopyOption.REPLACE_EXISTING);
                                }
                            } catch (IOException ex2) {
                                throw ex = ex2;
                            } finally {
                                try {
                                    temp.release();
                                } catch (IOException ex2) {
                                    throw (IOException) ex2.initCause(ex);
                                }
                            }
                        }
                    } finally {
                        final Entry template = FileOutputSocket.this.template;
                        if (null != template) {
                            try {
                                getFileAttributeView(entryFile, BasicFileAttributeView.class)
                                        .setTimes(  getFileTime(template, WRITE),
                                                    getFileTime(template, READ),
                                                    getFileTime(template, CREATE));
                            } catch (IOException ex2) {
                                throw (IOException) ex2.initCause(ex);
                            }
                        }
                    }
                }
            }
        } // class OutputStream

        try {
            if (temp != entry && options.get(APPEND))
                IOSocket.copy(  entry.getInputSocket(),
                                temp.getOutputSocket());
            return new OutputStream();
        } catch (IOException cause) {
            if (temp != entry) {
                try {
                    temp.release();
                } catch (IOException ex) {
                    throw (IOException) ex.initCause(cause);
                }
            }
            throw cause;
        }
    }

    private static FileTime getFileTime(Entry entry, Access type) {
        long time = entry.getTime(type);
        return UNKNOWN == time ? null : FileTime.fromMillis(time);
    }
}
