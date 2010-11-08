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
 * Implements a write-through or write-back caching strategy for input and
 * output sockets.
 * <p>
 * Note that the cache is only effective when the input and output sockets
 * are <em>not</em> connected to a peer socket!
 * 
 * @param   <LT> The type of the <i>local target</i> for I/O operations.
 * @author  Christian Schlichtherle
 * @version $Id$
 */
final class DefaultCache<LT extends CommonEntry> implements IOCache<LT> {
    private final Pool<FileEntry, IOException> pool = TempFilePool.get();
    private final InputSocketProxy inputProxy;
    private final OutputSocketProxy outputProxy;
    private final Strategy factory;
    private Pool<Buffer, IOException> inputStrategy;
    private Pool<Buffer, IOException> outputStrategy;
    private Buffer buffer;

    DefaultCache(   final InputSocket <? extends LT> input,
                    final OutputSocket<? extends LT> output,
                    final Strategy factory) {
        if (null == factory)
            throw new NullPointerException();
        this.inputProxy  = null == input  ? null : new InputSocketProxy (input );
        this.outputProxy = null == output ? null : new OutputSocketProxy(output);
        this.factory = factory;
    }

    private Pool<Buffer, IOException> getInputStrategy() {
        return null != inputStrategy
                ? inputStrategy
                : (inputStrategy = new InputStrategy());
    }

    private Pool<Buffer, IOException> getOutputStrategy() {
        return null != outputStrategy
                ? outputStrategy
                : (outputStrategy = factory.newOutputStrategy(this));
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
        if (null != buffer) { // DCL is OK in this context!
            synchronized (DefaultCache.this) {
                if (null != buffer)
                    getOutputStrategy().release(buffer);
            }
        }
    }

    @Override
    public void clear() throws IOException {
        if (null != buffer) { // DCL is OK in this context!
            synchronized (DefaultCache.this) {
                final Buffer buffer = this.buffer;
                if (null != buffer) {
                    // Order is important here!
                    this.buffer = null;
                    getInputStrategy().release(buffer);
                }
            }
        }
    }

    final class InputStrategy implements Pool<Buffer, IOException> {
        @Override
        public Buffer allocate() throws IOException {
            synchronized (DefaultCache.this) {
                if (null == buffer) {
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
                    buffer = new Buffer(output.getTemp());
                }
                buffer.used++;
                return buffer;
            }
        }

        @Override
        public void release(final Buffer buf) throws IOException {
            synchronized (DefaultCache.this) {
                buf.used--;
                if (buf != buffer && 0 == buf.used)
                    pool.release(buf.file);
            }
        }
    } // class InputStrategy

    abstract class OutputStrategy implements Pool<Buffer, IOException> {
        @Override
        public Buffer allocate() throws IOException {
            return new Buffer(pool.allocate());
        }

        @Override
        public void release(final Buffer buf) throws IOException {
            try {
                final OutputSocket<? extends LT> output
                        = outputProxy.getBoundSocket();
                assert null == output.getPeerTarget();
                class ProxyInput extends FilterInputSocket<CommonEntry> {
                    ProxyInput() {
                        super(FileInputSocket.get(buf.file));
                    }

                    @Override
                    public CommonEntry getLocalTarget() throws IOException {
                        return CommonEntry.NULL;
                    }
                }
                IOSocket.copy(new ProxyInput(), output);
            } catch (IOException ex) {
                pool.release(buf.file);
                throw ex;
            }
        }
    } // class OutputStrategy

    final class WriteBackOutputStrategy extends OutputStrategy {
        @Override
        public Buffer allocate() throws IOException {
            synchronized (DefaultCache.this) {
                final Buffer buf = super.allocate();
                buf.dirty = true;
                return buf;
            }
        }

        @Override
        public void release(final Buffer buf) throws IOException {
            if (!buf.dirty)
                return;
            synchronized (DefaultCache.this) {
                if (buf != buffer) {
                    buffer = buf;
                } else {
                    buf.dirty = false;
                    super.release(buf);
                }
            }
        }
    } // class WriteBackOutputStrategy

    final class WriteThroughOutputStrategy extends OutputStrategy {
        @Override
        public void release(final Buffer buf) throws IOException {
            if (buf != buffer) { // DCL is OK in this context!
                synchronized (DefaultCache.this) {
                    if (buf != buffer) {
                        buffer = buf;
                        super.release(buf);
                    }
                }
            }
        }
    } // class WriteThroughOutputStrategy

    final class Buffer {
        final FileEntry file;
        int used;
        volatile boolean dirty;

        Buffer(final FileEntry file) {
            this.file = file;
        }

        final class BufferReadOnlyFile extends FilterReadOnlyFile {
            boolean closed;

            BufferReadOnlyFile() throws IOException {
                super(FileInputSocket.get(file).newReadOnlyFile());
            }

            @Override
            public void close() throws IOException {
                if (closed)
                    return;
                closed = true;
                try {
                    rof.close();
                } finally {
                    getInputStrategy().release(Buffer.this);
                }
            }
        } // class BufferReadOnlyFile

        final class BufferInputStream extends FilterInputStream { // Do NOT extend FileIn|OutputStream: They implement finalize(), which may cause deadlocks!
            boolean closed;

            BufferInputStream() throws IOException {
                super(FileInputSocket.get(file).newInputStream());
            }

            @Override
            public void close() throws IOException {
                if (closed)
                    return;
                closed = true;
                try {
                    in.close();
                } finally {
                    getInputStrategy().release(Buffer.this);
                }
            }
        } // class BufferInputStream

        final class BufferOutputStream extends FilterOutputStream { // Do NOT extend FileIn|OutputStream: They implement finalize(), which may cause deadlocks!
            boolean closed;

            BufferOutputStream() throws IOException {
                super(FileOutputSocket.get(file).newOutputStream());
            }

            @Override
            public void close() throws IOException {
                if (closed)
                    return;
                closed = true;
                try {
                    out.close();
                } finally {
                    getOutputStrategy().release(Buffer.this);
                }
            }
        } // class BufferOutputStream
    } // class Buffer

    private final class InputSocketProxy extends FilterInputSocket<LT> {
        InputSocketProxy(final InputSocket <? extends LT> input) {
            super(input);
        }

        @Override
        public ReadOnlyFile newReadOnlyFile() throws IOException {
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
                return getBoundSocket().newReadOnlyFile();
            }
            return getInputStrategy().allocate().new BufferReadOnlyFile();
        }

        @Override
        public InputStream newInputStream() throws IOException {
            if (null != getPeerTarget()) {
                // Dito.
                flush();
                return getBoundSocket().newInputStream();
            }
            return getInputStrategy().allocate().new BufferInputStream();
        }
    } // class InputProxy

    private final class OutputSocketProxy extends FilterOutputSocket<LT> {
        OutputSocketProxy(final OutputSocket<? extends LT> output) {
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
            return getOutputStrategy().allocate().new BufferOutputStream();
        }
    } // class OutputProxy
}
