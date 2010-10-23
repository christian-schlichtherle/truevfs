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
import de.schlichtherle.truezip.util.Pool;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Implements a write-back caching strategy for input and output sockets.
 * Using this class has the following effects:
 * <ul>
 * <li>Upon the first read operation, the data will be read and from the
 *     local target and stored in the cache.
 *     Subsequent or concurrent read operations will be served from the cache
 *     without re-reading the data from the local target again until the cache
 *     gets cleared.
 * <li>Any data written to the cache will get written to the local target if
 *     and only if the cache gets flushed.
 * <li>After a write operation, the data will be stored in the cache for
 *     subsequent read operations until the cache gets cleared.
 * </ul>
 * <p>
 * Note that the cache is only effective when the input and output sockets
 * are <em>not</em> connected to a peer socket!
 * 
 * @param   <LT> The type of the <i>local target</i> for I/O operations.
 * @author  Christian Schlichtherle
 * @version $Id$
 */
final class DefaultCache<LT extends CommonEntry> implements Cache<LT> {
    private final Lock lock = new Lock();
    private final Pool<FileEntry, IOException> pool = TempFilePool.get();
    private final InputProxy inputProxy;
    final Pool<Buffer, IOException> inputBufferPool;
    private final OutputProxy outputProxy;
    final Pool<Buffer, IOException> outputBufferPool;
    private volatile Buffer buffer;

    DefaultCache(   final InputSocket <? extends LT> input,
                    final OutputSocket<? extends LT> output,
                    final Strategy strategy) {
        if (null == input) {
            this.inputProxy = null;
            this.inputBufferPool = null;
        } else {
            this.inputProxy = new InputProxy(input);
            this.inputBufferPool = new InputBufferPool();
        }
        if (null == output) {
            this.outputProxy = null;
            this.outputBufferPool = null;
        } else {
            this.outputProxy = new OutputProxy(output);
            this.outputBufferPool = strategy.newOutputBufferPool(this);
        }
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
        synchronized (lock) {
            if (null != buffer)
                outputBufferPool.release(buffer);
        }
    }

    @Override
    public void clear() throws IOException {
        synchronized (lock) {
            final Buffer buffer = this.buffer;
            if (null != buffer) {
                // Order is important here!
                this.buffer = null;
                inputBufferPool.release(buffer);
            }
        }
    }

    final class InputBufferPool implements Pool<Buffer, IOException> {
        @Override
        public Buffer allocate() throws IOException {
            synchronized (lock) {
                if (null == buffer) {
                    buffer = new Buffer();
                    final InputSocket<? extends LT> input
                            = inputProxy.getBoundSocket();
                    assert null == input.getPeerTarget();
                    class ProxyOutput extends OutputSocket<CommonEntry> {
                        FileEntry temp;

                        FileEntry getTemp() throws IOException {
                            return null != temp ? temp : (temp = pool.allocate());
                        }

                        @Override
                        public CommonEntry getLocalTarget() throws IOException {
                            return CommonEntry.NULL;
                        }

                        @Override
                        public OutputStream newOutputStream() throws IOException {
                            return FileOutputSocket.get(getTemp()).newOutputStream();
                        }
                    }
                    final ProxyOutput output = new ProxyOutput();
                    IOSocket.copy(input, output);
                    buffer.temp = output.getTemp();
                }
                buffer.used++;
                return buffer;
            }
        }

        @Override
        public void release(final Buffer resource) throws IOException {
            synchronized (lock) {
                resource.used--;
                if (resource != buffer && 0 >= resource.used) {
                    final FileEntry temp = resource.temp;
                    if (null != temp) {
                        resource.temp = null;
                        pool.release(temp);
                    }
                }
            }
        }
    } // class InputBufferPool

    abstract class OutputBufferPool implements Pool<Buffer, IOException> {
        @Override
        public Buffer allocate() throws IOException {
            final Buffer buffer = new Buffer();
            buffer.temp = pool.allocate();
            return buffer;
        }

        @Override
        public void release(final Buffer resource) throws IOException {
            try {
                final OutputSocket<? extends LT> output
                        = outputProxy.getBoundSocket();
                assert null == output.getPeerTarget();
                class ProxyInput extends FilterInputSocket<CommonEntry> {
                    ProxyInput() {
                        super(FileInputSocket.get(resource.temp));
                    }

                    @Override
                    public CommonEntry getLocalTarget() throws IOException {
                        return CommonEntry.NULL;
                    }
                }
                IOSocket.copy(new ProxyInput(), output);
            } catch (IOException ex) {
                pool.release(resource.temp);
                throw ex;
            }
        }
    } // class OutputBufferPool

    final class WriteBackOutputBufferPool extends OutputBufferPool {
        @Override
        public Buffer allocate() throws IOException {
            synchronized (lock) {
                final Buffer buffer = super.allocate();
                buffer.dirty = true;
                return buffer;
            }
        }

        @Override
        public void release(final Buffer resource) throws IOException {
            if (!resource.dirty)
                return;
            synchronized (lock) {
                if (resource != buffer) {
                    buffer = resource;
                } else {
                    resource.dirty = false;
                    super.release(resource);
                }
            }
        }
    } // class WriteBackOutputBufferPool

    final class WriteThroughOutputBufferPool extends OutputBufferPool {
        @Override
        public void release(final Buffer resource) throws IOException {
            synchronized (lock) {
                if (resource != buffer) {
                    buffer = resource;
                    super.release(resource);
                }
            }
        }
    } // class WriteThroughOutputBufferPool

    final class Buffer {
        FileEntry temp;
        int used;
        boolean dirty;

        final class BufferReadOnlyFile extends FilterReadOnlyFile {
            boolean closed;

            BufferReadOnlyFile() throws IOException {
                super(FileInputSocket.get(temp).newReadOnlyFile());
            }

            @Override
            public void close() throws IOException {
                if (closed)
                    return;
                closed = true;
                try {
                    rof.close();
                } finally {
                    inputBufferPool.release(Buffer.this);
                }
            }
        } // class ReadOnlyFile

        final class BufferInputStream extends FilterInputStream { // Do NOT extend FileIn|OutputStream: They implement finalize(), which may cause deadlocks!
            boolean closed;

            BufferInputStream() throws IOException {
                super(FileInputSocket.get(temp).newInputStream());
            }

            @Override
            public void close() throws IOException {
                if (closed)
                    return;
                closed = true;
                try {
                    in.close();
                } finally {
                    inputBufferPool.release(Buffer.this);
                }
            }
        } // class InputStream

        final class BufferOutputStream extends FilterOutputStream { // Do NOT extend FileIn|OutputStream: They implement finalize(), which may cause deadlocks!
            boolean closed;

            BufferOutputStream() throws IOException {
                super(FileOutputSocket.get(temp).newOutputStream());
            }

            @Override
            public void close() throws IOException {
                if (closed)
                    return;
                closed = true;
                try {
                    out.close();
                } finally {
                    outputBufferPool.release(Buffer.this);
                }
            }
        } // class OutputStream
    } // class Buffer

    private final class InputProxy extends FilterInputSocket<LT> {
        InputProxy(final InputSocket <? extends LT> input) {
            super(input);
        }

        @Override
        public ReadOnlyFile newReadOnlyFile() throws IOException {
            if (null != getPeerTarget()) {
                // Dito.
                flush();
                return getBoundSocket().newReadOnlyFile();
            }
            return inputBufferPool.allocate().new BufferReadOnlyFile();
        }

        @Override
        public InputStream newInputStream() throws IOException {
            if (null != getPeerTarget()) {
                // The data for connected sockets cannot not be cached because
                // sockets may transfer different encoded data depending on
                // the identity of their peer target!
                // E.g. if the ZipDriver recognizes a ZipEntry as its peer
                // target, it transfers deflated data in order to omit
                // redundant inflating of the data from the source archive file
                // and deflating it again to the target archive file.
                // So we must flush and bypass the cache.
                flush();
                return getBoundSocket().newInputStream();
            }
            return inputBufferPool.allocate().new BufferInputStream();
        }
    } // class InputProxy

    private final class OutputProxy extends FilterOutputSocket<LT> {
        OutputProxy(final OutputSocket<? extends LT> output) {
            super(output);
        }

        @Override
        @SuppressWarnings("ThrowableInitCause")
        public OutputStream newOutputStream() throws IOException {
            if (null != getPeerTarget()) {
                // Dito, but this time we must clear the cache.
                clear();
                return getBoundSocket().newOutputStream();
            }
            return outputBufferPool.allocate().new BufferOutputStream();
        }
    } // class OutputProxy

    private static class Lock {
    }
}
