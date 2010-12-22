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
package de.schlichtherle.truezip.io.filesystem.file;

import de.schlichtherle.truezip.io.entry.Entry;
import de.schlichtherle.truezip.io.DecoratingOutputStream;
import de.schlichtherle.truezip.io.filesystem.OutputOption;
import de.schlichtherle.truezip.io.socket.IOPool;
import de.schlichtherle.truezip.io.socket.IOSocket;
import de.schlichtherle.truezip.io.socket.OutputSocket;
import de.schlichtherle.truezip.util.BitField;
import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import static de.schlichtherle.truezip.io.filesystem.OutputOption.APPEND;
import static de.schlichtherle.truezip.io.filesystem.OutputOption.BUFFER;
import static de.schlichtherle.truezip.io.filesystem.OutputOption.CREATE_PARENTS;
import static de.schlichtherle.truezip.io.entry.Entry.Access.WRITE;
import static de.schlichtherle.truezip.io.entry.Entry.UNKNOWN;

/**
 * @see     FileInputSocket
 * @author  Christian Schlichtherle
 * @version $Id$
 */
final class FileOutputSocket extends OutputSocket<FileEntry> {

    private static final String TEMP_FILE_POOL_PREFIX = ".tzp";

    private final FileEntry file;
    private final BitField<OutputOption> options;
    private final Entry template;
    private volatile TempFilePool pool;

    FileOutputSocket(   @NonNull final FileEntry file,
                        @NonNull final BitField<OutputOption> options,
                        @Nullable final Entry template) {
        assert null != file;
        assert null != options;
        this.file = file;
        this.template = template;
        this.options = options;
    }

    private TempFilePool getTempFilePool() {
        if (null == pool)
            pool = new TempFilePool(    TEMP_FILE_POOL_PREFIX, null,
                                        file.getFile().getParentFile());
        return pool;
    }

    @Override
    public FileEntry getLocalTarget() {
        return file;
    }

    @Override
    @edu.umd.cs.findbugs.annotations.SuppressWarnings("RV_RETURN_VALUE_IGNORED_BAD_PRACTICE")
    public OutputStream newOutputStream() throws IOException {
        final File fileTarget = file.getFile();
        if (options.get(CREATE_PARENTS))
            fileTarget.getParentFile().mkdirs();
        final FileEntry temp = options.get(BUFFER) && !fileTarget.createNewFile()
                ? getTempFilePool().allocate()
                : file;
        final File tempTarget = temp.getFile();

        class OutputStream extends DecoratingOutputStream {
            boolean closed;

            OutputStream() throws FileNotFoundException {
                super(new FileOutputStream(tempTarget, options.get(APPEND))); // Do NOT extend FileIn|OutputStream: They implement finalize(), which may cause deadlocks!
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
                        if (temp != file) {
                            IOException cause = null;
                            try {
                                if (!tempTarget.renameTo(fileTarget)
                                        && (!fileTarget.delete() || !tempTarget.renameTo(fileTarget)))
                                    IOSocket.copy(  temp.getInputSocket(),
                                                    file.getOutputSocket());
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
                                    && !fileTarget.setLastModified(time))
                                throw new IOException(fileTarget.getPath() + " (cannot preserve last modification time)");
                        }
                    }
                }
            }
        } // class OutputStream

        try {
            if (temp != file && options.get(APPEND))
                IOSocket.copy(  file.getInputSocket(),
                                temp.getOutputSocket());
            return new OutputStream();
        } catch (IOException cause) {
            if (temp != file) {
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
