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
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * @see     BufferingInputSocket
 * @author  Christian Schlichtherle
 * @version $Id$
 */
public class BufferingOutputSocket<CE extends CommonEntry>
extends FilterOutputSocket<CE> {

    private final CommonEntryPool<FileEntry> pool;

    public BufferingOutputSocket(OutputSocket<? extends CE> output) {
        this(output, TempFilePool.get());
    }

    public BufferingOutputSocket(   final OutputSocket<? extends CE> output,
                                    final CommonEntryPool<FileEntry> creator) {
        super(output);
        if (null == creator)
            throw new NullPointerException();
        this.pool = creator;
    }

    @Override
    @SuppressWarnings("ThrowableInitCause")
    public OutputStream newOutputStream() throws IOException {
        final FileEntry temp = pool.allocate();

        class OutputStream extends FilterOutputStream {
            boolean closed;

            OutputStream() throws FileNotFoundException {
                super(new FileOutputStream(temp.getTarget())); // Do NOT extend FileIn|OutputStream: They implement finalize(), which may cause deadlocks!
            }

            @Override
            public void close() throws IOException {
                if (closed)
                    return;
                closed = true;
                try {
                    super.close();
                } finally {
                    CommonEntry remote = getRemoteTarget();
                    if (null == remote)
                        remote = temp;
                        IOException cause = null;
                    try {
                        IOSocket.copy(  FileInputSocket.get(temp, remote),
                                        getOutputSocket());
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
            }
        }

        try {
            return new OutputStream();
        } catch (IOException cause) {
            try {
                pool.release(temp);
            } catch (IOException ex) {
                throw (IOException) ex.initCause(cause);
            }
            throw cause;
        }
    }
}
