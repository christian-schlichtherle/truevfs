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
import de.schlichtherle.truezip.io.entry.FileEntry;
import de.schlichtherle.truezip.io.entry.TempFilePool;
import de.schlichtherle.truezip.io.rof.FilterReadOnlyFile;
import de.schlichtherle.truezip.io.rof.ReadOnlyFile;
import de.schlichtherle.truezip.io.rof.SimpleReadOnlyFile;
import de.schlichtherle.truezip.util.Pool;
import java.io.Closeable;
import java.io.File;
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
// FIXME: Current write policy is write-through. Implement write-back for better performance.
final class DefaultCache<LT extends CommonEntry> implements Cache<LT> {

    private final Pool<FileEntry, IOException> pool = TempFilePool.get();
    private final InputProxy inputProxy;
    private final OutputProxy outputProxy;
    private volatile Buffer buffer;
    private final Lock lock = new Lock();

    DefaultCache(   final InputSocket <? extends LT> input,
                    final OutputSocket<? extends LT> output) {
        this.inputProxy = new InputProxy(input);
        this.outputProxy = new OutputProxy(output);
    }

    @Override
    public InputSocket<LT> getInputSocket() {
        return inputProxy;
    }

    @Override
    public OutputSocket<LT> getOutputSocket() {
        return outputProxy;
    }

    @Override
    public void flush() throws IOException {
    }

    @Override
    public void clear() throws IOException {
        synchronized (lock) {
            Buffer buffer = this.buffer;
            if (null != buffer) {
                // Order is important here!
                this.buffer = null;
                buffer.inputPool.release(null);
            }
        }
    }

    private Buffer getBuffer() {
        synchronized (lock) {
            if (null == buffer)
                buffer = new Buffer();
            return buffer;
        }
    }

    private void release(   final Closeable closeable,
                            final Pool<Buffer, IOException> pool)
    throws IOException {
        synchronized (lock) {
            try {
                closeable.close();
            } finally {
                pool.release(null);
            }
        }
    }

    private static class Lock {
    }

    private final class Buffer {
        final InputPool inputPool = new InputPool();
        final OutputPool outputPool = new OutputPool();
        FileEntry temp;

        File getFile() {
            return temp.getFile();
        }

        @Override
        public String toString() {
            return null == temp ? "(null)" : temp.toString();
        }

        class InputPool implements Pool<Buffer, IOException> {
            int uses;

            @Override
            public Buffer allocate() throws IOException {
                synchronized (lock) {
                    if (null == temp) {
                        final InputSocket<? extends LT> input
                                = inputProxy.getBoundSocket();
                        CommonEntry peer = input.getPeerTarget();
                        final FileEntry temp = pool.allocate();
                        if (null == peer)
                            peer = temp;
                        try {
                            IOSocket.copy(  input,
                                            new ProxyingOutputSocket<CommonEntry>(peer,
                                                FileOutputSocket.get(temp)));
                        } catch (IOException ex) {
                            pool.release(temp);
                            throw ex;
                        }
                        Buffer.this.temp = temp;
                    }
                    uses++;
                }
                return Buffer.this;
            }

            @Override
            public void release(final Buffer resource) throws IOException {
                assert null == resource;
                synchronized (lock) {
                    uses--;
                    if (Buffer.this != buffer && 0 >= uses) {
                        final FileEntry temp = Buffer.this.temp;
                        Buffer.this.temp = null;
                        if (null != temp)
                            pool.release(temp);
                    }
                }
            }
        } // class InputBuffer

        class OutputPool implements Pool<Buffer, IOException> {
            @Override
            public Buffer allocate() throws IOException {
                assert null == temp;
                synchronized (lock) {
                    temp = pool.allocate();
                }
                return Buffer.this;
            }

            @Override
            public void release(final Buffer resource) throws IOException {
                assert null == resource;
                synchronized (lock) {
                    try {
                        final OutputSocket<? extends LT> output
                                = outputProxy.getBoundSocket();
                        CommonEntry peer = output.getPeerTarget();
                        if (null == peer)
                            peer = temp;
                        buffer = new Buffer();
                        IOSocket.copy(  new ProxyingInputSocket<CommonEntry>(peer,
                                            FileInputSocket.get(temp)),
                                        output);
                    } catch (IOException ex) {
                        pool.release(temp);
                        throw ex;
                    }
                    buffer.temp = temp;
                }
            }
        } // class OutputBuffer
    } // class Buffer

    private class InputProxy extends FilterInputSocket<LT> {
        InputProxy(final InputSocket <? extends LT> input) {
            super(input);
        }

        @Override
        public InputStream newInputStream() throws IOException {
            if (null != getPeerTarget()) {
                buffer = null;
                return super.newInputStream(); // can't cache data for connected sockets!
            }

            final Buffer buffer = getBuffer().inputPool.allocate();

            class InputStream extends FilterInputStream {
                boolean closed;

                InputStream() throws FileNotFoundException {
                    super(new FileInputStream(buffer.getFile())); // Do NOT extend FileIn|OutputStream: They implement finalize(), which may cause deadlocks!
                }

                @Override
                public void close() throws IOException {
                    if (closed)
                        return;
                    closed = true;
                    release(in, buffer.inputPool);
                }
            } // class InputStream

            return new InputStream();
        }

        @Override
        public ReadOnlyFile newReadOnlyFile() throws IOException {
            if (null != getPeerTarget()) {
                buffer = null;
                return super.newReadOnlyFile(); // can't cache data for connected sockets!
            }

            final Buffer buffer = getBuffer().inputPool.allocate();

            class ReadOnlyFile extends FilterReadOnlyFile {
                boolean closed;

                ReadOnlyFile() throws FileNotFoundException {
                    super(new SimpleReadOnlyFile(buffer.getFile()));
                }

                @Override
                public void close() throws IOException {
                    if (closed)
                        return;
                    closed = true;
                    release(rof, buffer.inputPool);
                }
            } // class ReadOnlyFile

            return new ReadOnlyFile();
        }
    } // class Input

    private class OutputProxy extends FilterOutputSocket<LT> {
        OutputProxy(final OutputSocket<? extends LT> output) {
            super(output);
        }

        @Override
        @SuppressWarnings("ThrowableInitCause")
        public OutputStream newOutputStream() throws IOException {
            if (null != getPeerTarget()) {
                buffer = null;
                return super.newOutputStream(); // can't cache data for connected sockets!
            }

            final Buffer buffer = new Buffer().outputPool.allocate();

            class OutputStream extends FilterOutputStream {
                boolean closed;

                OutputStream() throws FileNotFoundException {
                    super(new FileOutputStream(buffer.getFile())); // Do NOT extend FileIn|OutputStream: They implement finalize(), which may cause deadlocks!
                }

                @Override
                public void close() throws IOException {
                    if (closed)
                        return;
                    closed = true;
                    release(out, buffer.outputPool);
                }
            } // class OutputStream

            try {
                return new OutputStream();
            } catch (IOException ex) {
                assert false : ex;
                buffer.inputPool.release(null); // Dirty Hacky was here!
                throw ex;
            }
        }
    } // class Output
}
