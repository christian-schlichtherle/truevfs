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

import de.schlichtherle.truezip.io.FilterInputStream;
import de.schlichtherle.truezip.io.rof.ReadOnlyFile;
import de.schlichtherle.truezip.io.rof.SimpleReadOnlyFile;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

/**
 * @see     BufferingOutputSocket
 * @author  Christian Schlichtherle
 * @version $Id$
 */
public class BufferingInputSocket<CE extends CommonEntry>
extends FilterInputSocket<CE> {

    private final CommonEntryPool<FileEntry> pool;

    public BufferingInputSocket(InputSocket<? extends CE> input) {
        this(input, null);
    }

    public BufferingInputSocket(final InputSocket<? extends CE> input,
                                final CommonEntryPool<FileEntry> pool) {
        super(input);
        this.pool = null != pool ? pool : TempFilePool.get();
    }

    @SuppressWarnings("ThrowableInitCause")
    private FileEntry createTemporaryInputFile() throws IOException {
        final FileEntry temp = pool.allocate();
        try {
            CommonEntry remote = getRemoteTarget();
            if (null == remote)
                remote = temp;
            IOSocket.copy(  getInputSocket(),
                            new TargetOutputSocket<CommonEntry>(remote,
                                FileOutputSocket.get(temp)));
        } catch (IOException cause) {
            try {
                pool.release(temp);
            } catch (IOException ex) {
                throw (IOException) ex.initCause(cause);
            }
            throw cause;
        }
        return temp;
    }

    @Override
    public InputStream newInputStream() throws IOException {
        final FileEntry temp = createTemporaryInputFile();

        class InputStream extends FilterInputStream {
            boolean closed;

            InputStream() throws FileNotFoundException {
                super(new FileInputStream(temp.getTarget())); // Do NOT extend FileIn|OutputStream: They implement finalize(), which may cause deadlocks!
            }

            @Override
            @SuppressWarnings("ThrowableInitCause")
            public void close() throws IOException {
                if (closed)
                    return;
                closed = true;
                IOException cause = null;
                try {
                    try {
                        super.close();
                    } catch (IOException ex) {
                        throw cause = ex;
                    }
                } finally {
                    pool.release(temp);
                }
            }
        }

        return new InputStream();
    }

    @Override
    public ReadOnlyFile newReadOnlyFile() throws IOException {
        final FileEntry temp = createTemporaryInputFile();

        class ReadOnlyFile extends SimpleReadOnlyFile {
            boolean closed;

            ReadOnlyFile() throws FileNotFoundException {
                super(temp.getTarget());
            }

            @Override
            @SuppressWarnings("ThrowableInitCause")
            public void close() throws IOException {
                if (closed)
                    return;
                closed = true;
                IOException cause = null;
                try {
                    try {
                        super.close();
                    } catch (IOException ex) {
                        throw cause = ex;
                    }
                } finally {
                    pool.release(temp);
                }
            }
        }

        return new ReadOnlyFile();
    }
}
