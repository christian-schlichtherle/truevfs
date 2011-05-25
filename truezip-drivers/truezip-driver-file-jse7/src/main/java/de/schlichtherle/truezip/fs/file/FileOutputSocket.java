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
package de.schlichtherle.truezip.fs.file;

import edu.umd.cs.findbugs.annotations.DefaultAnnotation;
import de.schlichtherle.truezip.entry.Entry;
import de.schlichtherle.truezip.io.DecoratingOutputStream;
import de.schlichtherle.truezip.fs.FsOutputOption;
import de.schlichtherle.truezip.socket.IOPool;
import de.schlichtherle.truezip.socket.IOSocket;
import de.schlichtherle.truezip.socket.OutputSocket;
import de.schlichtherle.truezip.util.BitField;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.NonNull;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import static java.nio.file.Files.*;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
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

    private static final String FILE_POOL_PREFIX = ".tzp";
    private static final StandardOpenOption[] 
            NO_STANDARD_OPEN_OPTION = { };
    private static final StandardOpenOption[]
            APPEND_STANDARD_OPEN_OPTION = { StandardOpenOption.APPEND };

    private final                  FileEntry                entry;
    private final                  BitField<FsOutputOption> options;
    private final    @CheckForNull Entry                    template;
    private volatile @CheckForNull TempFilePool             pool;

    FileOutputSocket(   final               FileEntry                entry,
                        final               BitField<FsOutputOption> options,
                        final @CheckForNull Entry                    template) {
        assert null != entry;
        assert null != options;
        this.entry    = entry;
        this.options  = options;
        this.template = template;
    }

    private TempFilePool getTempFilePool() {
        final TempFilePool pool = this.pool;
        return null != pool
                ? pool
                : (this.pool = new TempFilePool(FILE_POOL_PREFIX, null,
                                                entry.getPath().getParent()));
    }

    @Override
    public FileEntry getLocalTarget() {
        return entry;
    }

    @SuppressWarnings("unchecked")
    @Override
    @edu.umd.cs.findbugs.annotations.SuppressWarnings("RV_RETURN_VALUE_IGNORED_BAD_PRACTICE")
    public OutputStream newOutputStream() throws IOException {
        final Path entryFile = entry.getPath();
        if (options.get(EXCLUSIVE) && exists(entryFile))
            throw new IOException(entryFile + " (file exists already)"); // this is obviously not atomic
        if (options.get(CREATE_PARENTS))
            createDirectories(entryFile.getParent());
        FileAlreadyExistsException exists = null;
        if (options.get(CACHE)) {
            try {
                createFile(entryFile);
            } catch (FileAlreadyExistsException ex) {
                exists = ex;
            }
        }
        final FileEntry temp = null != exists
                ? getTempFilePool().allocate()
                : entry;
        final Path tempFile = temp.getPath();

        class OutputStream extends DecoratingOutputStream {
            boolean closed;

            OutputStream() throws IOException {
                super(newOutputStream(tempFile,
                        options.get(APPEND)
                            ? APPEND_STANDARD_OPEN_OPTION
                            : NO_STANDARD_OPEN_OPTION));
            }

            @Override
            public void close() throws IOException {
                if (closed)
                    return;
                closed = true;
                try {
                    super.close();
                } finally {
                    try {
                        if (temp != entry) {
                            IOException cause = null;
                            try {
                                try {
                                    move(tempFile, entryFile,
                                            StandardCopyOption.REPLACE_EXISTING);
                                } catch (IOException ex) {
                                    Files.copy(tempFile, entryFile,
                                            StandardCopyOption.REPLACE_EXISTING);
                                }
                            } catch (IOException ex) {
                                throw cause = ex;
                            } finally {
                                try {
                                    ((IOPool.Entry<FileEntry>) temp).release();
                                } catch (IOException ex) {
                                    throw (IOException) ex.initCause(cause);
                                }
                            }
                        }
                    } finally {
                        final Entry template = FileOutputSocket.this.template;
                        if (null != template) {
                            final long time = template.getTime(WRITE);
                            if (UNKNOWN != time)
                                Files.setLastModifiedTime(
                                        entryFile, FileTime.fromMillis(time));
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
                    ((IOPool.Entry<FileEntry>) temp).release();
                } catch (IOException ex) {
                    throw (IOException) ex.initCause(cause);
                }
            }
            throw cause;
        }
    }
}
