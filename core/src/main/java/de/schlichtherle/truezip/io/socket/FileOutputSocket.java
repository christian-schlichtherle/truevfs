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
package de.schlichtherle.truezip.io.socket;

import de.schlichtherle.truezip.io.FilterOutputStream;
import de.schlichtherle.truezip.util.BitField;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import static de.schlichtherle.truezip.io.socket.OutputOption.APPEND;
import static de.schlichtherle.truezip.io.socket.OutputOption.BUFFER;
import static de.schlichtherle.truezip.io.socket.OutputOption.CREATE_PARENTS;
import static de.schlichtherle.truezip.io.socket.OutputOption.COPY_PROPERTIES;
import static de.schlichtherle.truezip.io.socket.CommonEntry.Access.WRITE;
import static de.schlichtherle.truezip.io.socket.CommonEntry.UNKNOWN;

/**
 * @see     FileInputSocket
 * @author  Christian Schlichtherle
 * @version $Id$
 */
final class FileOutputSocket<CE extends CommonEntry>
extends OutputSocket<CE> {
    private final FileEntry file;
    private final BitField<OutputOption> options;
    private final CE local;
    private final CommonEntryPool<FileEntry> pool;

    static FileOutputSocket<FileEntry> get(FileEntry file, BitField<OutputOption> options) {
        return new FileOutputSocket<FileEntry>(file, options, file);
    }

    static <CE extends CommonEntry> FileOutputSocket<CE> get(FileEntry file, CE local) {
        return new FileOutputSocket<CE>(file, BitField.noneOf(OutputOption.class), local);
    }

    FileOutputSocket(   final FileEntry file,
                        final BitField<OutputOption> options,
                        final CE local) {
        if (null == local || null == file || null == options)
            throw new NullPointerException();
        this.local = local;
        this.file = file;
        this.options = options;
        final File fileTarget = file.getTarget();
        this.pool = new TempFilePool(   fileTarget.getName(),
                                        null,
                                        fileTarget.getParentFile());
    }

    @Override
    public CE getLocalTarget() {
        return local;
    }

    @Override
    @SuppressWarnings("ThrowableInitCause")
    public OutputStream newOutputStream() throws IOException {
        final File fileTarget = file.getTarget();
        if (options.get(CREATE_PARENTS))
            fileTarget.getParentFile().mkdirs();
        final FileEntry temp = options.get(BUFFER) && !fileTarget.createNewFile()
                ? pool.allocate()
                : file;
        final File tempTarget = temp.getTarget();

        class OutputStream extends FilterOutputStream {
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
                    final CommonEntry remote = getRemoteTarget();
                    if (temp != file
                    && !tempTarget.renameTo(fileTarget)
                    && (!fileTarget.delete() || !tempTarget.renameTo(fileTarget))) {
                        IOException cause = null;
                        try {
                            IOSocket.copy(  FileInputSocket.get(temp, local),
                                            FileOutputSocket.get(file, local));
                        } catch (IOException ex) {
                            throw cause = ex;
                        } finally {
                            try {
                                pool.release(temp);
                            } catch (IOException ex) {
                                throw (IOException) ex.initCause(cause);
                            }
                        }
                    }
                    if (options.get(COPY_PROPERTIES) && null != remote) {
                        final long time = remote.getTime(WRITE);
                        if (UNKNOWN != time)
                            if (!fileTarget.setLastModified(time))
                                throw new IOException(fileTarget.getPath() + " (cannot preserve last modification time)");
                    }
                }
            }
        }

        try {
            if (temp != file && options.get(APPEND))
                IOSocket.copy(  FileInputSocket.get(file, local),
                                FileOutputSocket.get(temp, local));
            return new OutputStream();
        } catch (IOException cause) {
            if (temp != file) {
                try {
                    pool.release(temp);
                } catch (IOException ex) {
                    throw (IOException) ex.initCause(cause);
                }
            }
            throw cause;
        }
    }
}
