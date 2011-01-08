/*
 * Copyright (C) 2010 Schlichtherle IT Services
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
package de.schlichtherle.truezip.io.fs.file;

import de.schlichtherle.truezip.io.entry.Entry;
import de.schlichtherle.truezip.io.DecoratorOutputStream;
import de.schlichtherle.truezip.io.fs.FsOutputOption;
import de.schlichtherle.truezip.io.socket.IOPool;
import de.schlichtherle.truezip.io.socket.IOSocket;
import de.schlichtherle.truezip.io.socket.OutputSocket;
import de.schlichtherle.truezip.util.BitField;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.NonNull;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import static de.schlichtherle.truezip.io.fs.FsOutputOption.APPEND;
import static de.schlichtherle.truezip.io.fs.FsOutputOption.CACHE;
import static de.schlichtherle.truezip.io.fs.FsOutputOption.CREATE_PARENTS;
import static de.schlichtherle.truezip.io.entry.Entry.Access.WRITE;
import static de.schlichtherle.truezip.io.entry.Entry.UNKNOWN;

/**
 * @see     FileInputSocket
 * @author  Christian Schlichtherle
 * @version $Id$
 */
final class FileOutputSocket extends OutputSocket<FileEntry> {

    private static final String FILE_POOL_PREFIX = ".tzp";

    private final    @NonNull      FileEntry              entry;
    private final    @NonNull      BitField<FsOutputOption> options;
    private final    @CheckForNull Entry                    template;
    private volatile @CheckForNull FilePool               pool;

    FileOutputSocket( final @NonNull      FileEntry              entry,
                        final @NonNull      BitField<FsOutputOption> options,
                        final @CheckForNull Entry                    template) {
        this.entry    = entry;
        this.options  = options;
        this.template = template;
    }

    private FilePool getTempFilePool() {
        return null != pool
                ? pool
                : (pool = new FilePool(   FILE_POOL_PREFIX, null,
                                            entry.getFile().getParentFile()));
    }

    @Override
    public FileEntry getLocalTarget() {
        return entry;
    }

    @Override
    @edu.umd.cs.findbugs.annotations.SuppressWarnings("RV_RETURN_VALUE_IGNORED_BAD_PRACTICE")
    public OutputStream newOutputStream() throws IOException {
        final File entryFile = entry.getFile();
        if (options.get(CREATE_PARENTS))
            entryFile.getParentFile().mkdirs();
        final FileEntry temp = options.get(CACHE) && !entryFile.createNewFile()
                ? getTempFilePool().allocate()
                : entry;
        final File tempFile = temp.getFile();

        @edu.umd.cs.findbugs.annotations.SuppressWarnings("NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE") // False positive with org.codehaus.mojo:findbugs-maven-plugin:2.3.1
        class OutputStream extends DecoratorOutputStream {
            boolean closed;

            OutputStream() throws FileNotFoundException {
                super(new FileOutputStream(tempFile, options.get(APPEND))); // Do NOT extend FileIn|OutputStream: They implement finalize(), which may cause deadlocks!
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
                                if (!tempFile.renameTo(entryFile)
                                        && (!entryFile.delete() || !tempFile.renameTo(entryFile)))
                                    IOSocket.copy(  temp.getInputSocket(),
                                                    entry.getOutputSocket());
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
                        if (null != template) {
                            final long time = template.getTime(WRITE);
                            if (UNKNOWN != time
                                    && !entryFile.setLastModified(time))
                                throw new IOException(entryFile.getPath() + " (cannot preserve last modification time)");
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
