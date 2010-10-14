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

import de.schlichtherle.truezip.io.entry.CommonEntry;
import de.schlichtherle.truezip.io.entry.CommonEntryPool;
import de.schlichtherle.truezip.io.entry.TempFilePool;
import de.schlichtherle.truezip.io.entry.FileEntry;
import de.schlichtherle.truezip.io.FilterOutputStream;
import de.schlichtherle.truezip.util.BitField;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import static de.schlichtherle.truezip.io.socket.OutputOption.APPEND;
import static de.schlichtherle.truezip.io.socket.OutputOption.CACHE;
import static de.schlichtherle.truezip.io.socket.OutputOption.CREATE_PARENTS;
import static de.schlichtherle.truezip.io.socket.OutputOption.COPY_PROPERTIES;
import static de.schlichtherle.truezip.io.entry.CommonEntry.Access.WRITE;
import static de.schlichtherle.truezip.io.entry.CommonEntry.UNKNOWN;

/**
 * @see     FileInputSocket
 * @author  Christian Schlichtherle
 * @version $Id$
 */
public final class FileOutputSocket extends OutputSocket<FileEntry> {
    private final FileEntry file;
    private final BitField<OutputOption> options;
    private final CommonEntryPool<FileEntry> pool;

    public static OutputSocket<FileEntry> get(FileEntry file) {
        return new FileOutputSocket(file, null);
    }

    public static OutputSocket<FileEntry> get(
            FileEntry file,
            BitField<OutputOption> options) {
        return new FileOutputSocket(file, options);
    }

    private FileOutputSocket(   final FileEntry file,
                                final BitField<OutputOption> options) {
        if (null == file)
            throw new NullPointerException();
        this.file = file;
        this.options = null != options ? options : BitField.noneOf(OutputOption.class);
        final File fileTarget = file.getTarget();
        this.pool = new TempFilePool(   fileTarget.getName(),
                                        null,
                                        fileTarget.getParentFile());
    }

    @Override
    public FileEntry getLocalTarget() {
        return file;
    }

    @Override
    @SuppressWarnings("ThrowableInitCause")
    public OutputStream newOutputStream() throws IOException {
        final File fileTarget = file.getTarget();
        if (options.get(CREATE_PARENTS))
            fileTarget.getParentFile().mkdirs();
        final FileEntry temp = options.get(CACHE) && !fileTarget.createNewFile()
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
                            IOSocket.copy(  FileInputSocket.get(temp),
                                            FileOutputSocket.get(file));
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
                IOSocket.copy(  FileInputSocket.get(file),
                                FileOutputSocket.get(temp));
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
