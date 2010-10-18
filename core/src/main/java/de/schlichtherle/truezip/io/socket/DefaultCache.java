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
import de.schlichtherle.truezip.io.FilterOutputStream;
import de.schlichtherle.truezip.io.entry.CommonEntry;
import de.schlichtherle.truezip.io.entry.CommonEntryPool;
import de.schlichtherle.truezip.io.entry.FileEntry;
import de.schlichtherle.truezip.io.entry.TempFilePool;
import de.schlichtherle.truezip.io.rof.FilterReadOnlyFile;
import de.schlichtherle.truezip.io.rof.ReadOnlyFile;
import de.schlichtherle.truezip.io.rof.SimpleReadOnlyFile;
import java.io.Closeable;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Default implementation of a cache strategy for input and output sockets.
 * 
 * @param   <LT> The type of the <i>local target</i> for I/O operations.
 * @author  Christian Schlichtherle
 * @version $Id$
 */
// FIXME: Make this work as described in the interface contract!
final class DefaultCache<LT extends CommonEntry> implements Cache<LT> {

    //private int  readCount; // shared
    //private int writeCount; // exclusive

    private final Input input;
    private final Output output;
    private final CommonEntryPool<FileEntry> pool = TempFilePool.get();

    DefaultCache(   final InputSocket <? extends LT> input,
                    final OutputSocket<? extends LT> output) {
        this.input = new Input(input);
        this.output = new Output(output);
    }

    @Override
    public InputSocket<LT> getInputSocket() throws IOException {
        return input;
    }

    @Override
    public OutputSocket<LT> getOutputSocket() throws IOException {
        return output;
    }

    @Override
    public void flush() throws IOException {
    }

    @Override
    public void clear() throws IOException {
    }

    private final class Input extends FilterInputSocket<LT> {
        Input(final InputSocket <? extends LT> input) {
            super(input);
        }

        @Override
        public InputStream newInputStream() throws IOException {
            final FileEntry temp = allocateInput();

            class InputStream extends FilterInputStream {
                boolean closed;

                InputStream() throws FileNotFoundException {
                    super(new FileInputStream(temp.getFile())); // Do NOT extend FileIn|OutputStream: They implement finalize(), which may cause deadlocks!
                }

                @Override
                public void close() throws IOException {
                    if (closed)
                        return;
                    closed = true;
                    releaseInput(in, temp);
                }
            } // class InputStream

            return new InputStream();
        }

        @Override
        public ReadOnlyFile newReadOnlyFile() throws IOException {
            final FileEntry temp = allocateInput();

            class ReadOnlyFile extends FilterReadOnlyFile {
                boolean closed;

                ReadOnlyFile() throws FileNotFoundException {
                    super(new SimpleReadOnlyFile(temp.getFile()));
                }

                @Override
                public void close() throws IOException {
                    if (closed)
                        return;
                    closed = true;
                    releaseInput(rof, temp);
                }
            } // class ReadOnlyFile

            return new ReadOnlyFile();
        }

        FileEntry allocateInput() throws IOException {
            final FileEntry temp = pool.allocate();
            try {
                CommonEntry peer = getPeerTarget();
                if (null == peer)
                    peer = temp;
                IOSocket.copy(  getInputSocket(),
                                new ProxyingOutputSocket<CommonEntry>(peer,
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

        void releaseInput(final Closeable closeable, final FileEntry temp)
        throws IOException {
            IOException cause = null;
            try {
                closeable.close();
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
    } // class Input

    private final class Output extends FilterOutputSocket<LT> {
        Output(final OutputSocket<? extends LT> output) {
            super(output);
        }

        @Override
        @SuppressWarnings("ThrowableInitCause")
        public OutputStream newOutputStream() throws IOException {
            final FileEntry temp = allocateOutput();

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
                    releaseOutput(out, temp);
                }
            } // class OutputStream

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

        FileEntry allocateOutput() throws IOException {
            return pool.allocate();
        }

        void releaseOutput(final Closeable closeable, final FileEntry temp)
        throws IOException {
            IOException cause = null;
            try {
                closeable.close();
            } catch (IOException ex) {
                throw cause = ex;
            } finally {
                try {
                    CommonEntry peer = getPeerTarget();
                    if (null == peer)
                        peer = temp;
                    IOSocket.copy(  new ProxyingInputSocket<CommonEntry>(peer,
                                        FileInputSocket.get(temp)),
                                    getOutputSocket());
                } catch (IOException ex) {
                    throw cause = (IOException) ex.initCause(cause);
                } finally {
                    try {
                        pool.release(temp);
                    } catch (IOException ex) {
                        throw (IOException) ex.initCause(cause);
                    }
                }
            }
        }
    } // class Output
}
