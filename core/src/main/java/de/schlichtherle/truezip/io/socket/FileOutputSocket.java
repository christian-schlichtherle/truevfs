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
    private final CE local;
    private final File file;
    private final BitField<OutputOption> options;

    FileOutputSocket(CE entry, File file) {
        this(entry, file, BitField.noneOf(OutputOption.class));
    }

    FileOutputSocket(   final CE entry,
                        final File file,
                        final BitField<OutputOption> options) {
        if (null == entry || null == file || null == options)
            throw new NullPointerException();
        this.local = entry;
        this.file = file;
        this.options = options;
    }

    @Override
    public CE getLocalTarget() {
        return local;
    }

    @Override
    @SuppressWarnings("ThrowableInitCause")
    public OutputStream newOutputStream() throws IOException {
        final File dir = file.getParentFile();
        if (options.get(CREATE_PARENTS))
            dir.mkdirs();
        final File temp = options.get(BUFFER) && !file.createNewFile()
                ? new TempFileCreator(file.getName(), null, dir).createFile()
                : file;
        if (temp != file && options.get(APPEND)) {
            try {
                IOSocket.copy(  new FileInputSocket <CE>(local, file),
                                new FileOutputSocket<CE>(local, temp));
            } catch (IOException ex) {
                if (!temp.delete())
                    throw (IOException) new IOException(temp.getPath() + " (cannot delete temporary output file)").initCause(ex);
                throw ex;
            }
        }

        class OutputStream extends FilterOutputStream {
            boolean closed;

            OutputStream() throws FileNotFoundException {
                super(new FileOutputStream(temp, options.get(APPEND))); // Do NOT extend FileIn|OutputStream: They implement finalize(), which may cause deadlocks!
            }

            @Override
            public void close() throws IOException {
                if (closed)
                    return;
                closed = true;
                try {
                    super.close();
                } finally {
                    final CommonEntry peer = getRemoteTarget();
                    if (temp != file
                    && !temp.renameTo(file)
                    && (!file.delete() || !temp.renameTo(file))) {
                        IOException cause = null;
                        try {
                            try {
                                IOSocket.copy(  new FileInputSocket <CE>(local, temp),
                                                new FileOutputSocket<CE>(local, file));
                            } catch (IOException ex) {
                                throw cause = ex;
                            }
                        } finally {
                            if (!temp.delete())
                                throw (IOException) new IOException(temp.getPath() + " (cannot delete temporary output file)").initCause(cause);
                        }
                    }
                    if (options.get(COPY_PROPERTIES) && null != peer) {
                        final long time = peer.getTime(WRITE);
                        if (UNKNOWN != time)
                            if (!file.setLastModified(time))
                                throw new IOException(file.getPath() + " (cannot preserve last modification time)");
                    }
                }
            }
        }

        try {
            return new OutputStream();
        } catch (IOException ex) {
            if (temp != file && !temp.delete())
                throw (IOException) new IOException(temp.getPath() + " (cannot delete temporary output file)").initCause(ex);
            throw ex;
        }
    }
}
