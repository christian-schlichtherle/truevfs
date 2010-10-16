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

import de.schlichtherle.truezip.io.entry.CommonEntryPool;
import de.schlichtherle.truezip.io.entry.TempFilePool;
import de.schlichtherle.truezip.io.entry.CommonEntry;
import de.schlichtherle.truezip.io.entry.FileEntry;
import de.schlichtherle.truezip.io.FilterOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * @see     CachingInputSocket
 * @author  Christian Schlichtherle
 * @version $Id$
 */
public final class CachingOutputSocket<LT extends CommonEntry>
extends FilterOutputSocket<LT> {

    private final CommonEntryPool<FileEntry> pool;

    public CachingOutputSocket(OutputSocket<? extends LT> output) {
        this(output, null);
    }

    public CachingOutputSocket( final OutputSocket<? extends LT> output,
                                final CommonEntryPool<FileEntry> pool) {
        super(output);
        this.pool = null != pool ? pool : TempFilePool.get();
    }

    @Override
    @SuppressWarnings("ThrowableInitCause")
    public OutputStream newOutputStream() throws IOException {
        final FileEntry temp = pool.allocate();

        class OutputStream extends FilterOutputStream {
            boolean closed;

            OutputStream() throws FileNotFoundException {
                super(new FileOutputStream(temp.getFile())); // Do NOT extend FileIn|OutputStream: They implement finalize(), which may cause deadlocks!
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
                        IOSocket.copy(  new ProxyingInputSocket<CommonEntry>(remote,
                                            FileInputSocket.get(temp)),
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
