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
package de.schlichtherle.truezip.io.filesystem.file;

import de.schlichtherle.truezip.io.DecoratingInputStream;
import de.schlichtherle.truezip.io.DecoratingOutputStream;
import de.schlichtherle.truezip.io.entry.Entry;
import de.schlichtherle.truezip.io.rof.DecoratingReadOnlyFile;
import de.schlichtherle.truezip.io.rof.ReadOnlyFile;
import de.schlichtherle.truezip.io.socket.DecoratingInputSocket;
import de.schlichtherle.truezip.io.socket.DecoratingOutputSocket;
import de.schlichtherle.truezip.io.socket.IOCache;
import de.schlichtherle.truezip.io.socket.IOPool;
import de.schlichtherle.truezip.io.socket.IOSocket;
import de.schlichtherle.truezip.io.socket.InputCache;
import de.schlichtherle.truezip.io.socket.InputSocket;
import de.schlichtherle.truezip.io.socket.OutputCache;
import de.schlichtherle.truezip.io.socket.OutputSocket;
import de.schlichtherle.truezip.util.Pool;
import edu.umd.cs.findbugs.annotations.NonNull;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import net.jcip.annotations.ThreadSafe;

/**
 * Implements a caching strategy for input and output sockets.
 * Using this interface has the following effects:
 * <ul>
 * <li>Upon the first read operation, the data will be read from the local
 *     target and temporarily stored in this cache.
 *     Subsequent or concurrent read operations will be served from this cache
 *     without re-reading the data from the local target again until this cache
 *     gets {@link InputCache#clear cleared}.</li>
 * <li>At the discretion of the implementation, data written to this cache may
 *     not be written to the local target until this cache gets
 *     {@link OutputCache#flush flushed}.</li>
 * <li>After a write operation, the data will be temporarily stored in this
 *     cache for subsequent read operations until this cache gets
 *     {@link OutputCache#clear cleared}.
 * </ul>
 *
 * @param   <LT> The type of the <i>local target</i> for I/O operations.
 * @author  Christian Schlichtherle
 * @version $Id$
 */
@ThreadSafe
public final class Cache<LT extends Entry> implements IOCache<LT> {

    /** Provides different cache strategies. */
    public enum Strategy {

        /**
         * Any attempt to obtain an output socket will result in a
         * {@link NullPointerException}.
         */
        READ_ONLY {
            @Override <LT extends Entry>
            Pool<Cache<LT>.Buffer, IOException> newOutputBufferPool(
                    Cache<LT> cache) {
                throw new AssertionError(); // should throw an NPE before we can get here!
            }
        },

        /**
         * A write-through cache flushes any written data as soon as the
         * output stream created by the provided output socket gets closed.
         */
        WRITE_THROUGH {
            @Override <LT extends Entry>
            Pool<Cache<LT>.Buffer, IOException> newOutputBufferPool(
                    Cache<LT> cache) {
                return cache.new WriteThroughOutputBufferPool();
            }
        },

        /**
         * A write-back cache flushes any written data if and only if it gets
         * explicitly flushed.
         */
        WRITE_BACK {
            @Override <LT extends Entry>
            Pool<Cache<LT>.Buffer, IOException> newOutputBufferPool(
                    Cache<LT> cache) {
                return cache.new WriteBackOutputBufferPool();
            }
        };

        /** Returns a new input / output cache. */
        @NonNull public <LT extends Entry>
        Cache<LT> newCache(Class<LT> clazz) {
            return new Cache<LT>(this);
        }

        @NonNull <LT extends Entry>
        Pool<Cache<LT>.Buffer, IOException> newInputBufferPool(
                Cache<LT> cache) {
            return cache.new InputBufferPool();
        }

        @NonNull abstract <LT extends Entry>
        Pool<Cache<LT>.Buffer, IOException> newOutputBufferPool(
                Cache<LT> cache);
    }

    private final Strategy strategy;
    private final IOPool<?> pool;
    private volatile InputSocket<? extends LT> input;
    private volatile OutputSocket<? extends LT> output;
    private volatile Pool<Buffer, IOException> inputBufferPool;
    private volatile Pool<Buffer, IOException> outputBufferPool;
    private volatile Buffer buffer;

    private Cache(@NonNull final Strategy strategy) {
        this(strategy, TempFilePool.get());
    }

    private Cache(@NonNull final Strategy strategy, @NonNull final IOPool<?> pool) {
        if (null == strategy || null == pool)
            throw new NullPointerException();
        this.strategy = strategy;
        this.pool = pool;
    }

    @NonNull
    public Cache<LT> configure(@NonNull final InputSocket <? extends LT> input) {
        if (null == input)
            throw new NullPointerException();
        this.input = input;
        return this;
    }

    @NonNull
    public Cache<LT> configure(@NonNull final OutputSocket <? extends LT> output) {
        if (null == output)
            throw new NullPointerException();
        this.output = output;
        return this;
    }

    @Override
    public InputSocket<LT> getInputSocket() {
        return new InputSocketProxy(input);
    }

    @Override
    public OutputSocket<LT> getOutputSocket() {
        return new OutputSocketProxy(output);
    }

    @Override
    public void flush() throws IOException {
        if (null == buffer) // DCL is OK in this context!
            return;
        synchronized (Cache.this) {
            final Buffer buffer = this.buffer;
            if (null != buffer)
                getOutputBufferPool().release(buffer);
        }
    }

    @Override
    public void clear() throws IOException {
        synchronized (Cache.this) {
            final Buffer buffer = this.buffer;
            this.buffer = null;
            if (null != buffer && 0 == buffer.reading && !buffer.dirty)
                buffer.release();
        }
    }

    private Pool<Buffer, IOException> getInputBufferPool() {
        return null != inputBufferPool
                ? inputBufferPool
                : (inputBufferPool = strategy.newInputBufferPool(this));
    }

    private final class InputBufferPool implements Pool<Buffer, IOException> {
        @Override
        public Buffer allocate() throws IOException {
            synchronized (Cache.this) {
                Buffer buffer = Cache.this.buffer;
                if (null == buffer) {
                    assert null == input.getPeerTarget();
                    class ProxyOutput extends OutputSocket<IOPool.Entry<?>> {
                        IOPool.Entry<?> temp;

                        @Override
                        public IOPool.Entry<?> getLocalTarget() throws IOException {
                            return null != temp ? temp : (temp = pool.allocate());
                        }

                        @Override
                        public OutputStream newOutputStream() throws IOException {
                            return getLocalTarget().getOutputSocket().newOutputStream();
                        }
                    }
                    final ProxyOutput output = new ProxyOutput();
                    IOSocket.copy(input, output);
                    Cache.this.buffer = buffer = new Buffer(output.getLocalTarget());
                }
                buffer.reading++;
                return buffer;
            }
        }

        @Override
        public void release(final Buffer buffer) throws IOException {
            synchronized (Cache.this) {
                if (0 == --buffer.reading && buffer != Cache.this.buffer && !buffer.dirty)
                    buffer.release();
            }
        }
    } // class InputBufferPool

    private Pool<Buffer, IOException> getOutputBufferPool() {
        return null != outputBufferPool
                ? outputBufferPool
                : (outputBufferPool = strategy.newOutputBufferPool(this));
    }

    private abstract class OutputBufferPool implements Pool<Buffer, IOException> {
        @Override
        public Buffer allocate() throws IOException {
            final Buffer buffer = new Buffer(pool.allocate());
            buffer.dirty = true;
            return buffer;
        }

        @Override
        public void release(final Buffer buffer) throws IOException {
            try {
                assert null == output.getPeerTarget();
                class ProxyInput extends InputSocket<IOPool.Entry<?>> {
                    @Override
                    public IOPool.Entry<?> getLocalTarget() throws IOException {
                        return buffer.file;
                    }

                    @Override
                    public ReadOnlyFile newReadOnlyFile() throws IOException {
                        return buffer.file.getInputSocket().newReadOnlyFile();
                    }

                    @Override
                    public InputStream newInputStream() throws IOException {
                        return buffer.file.getInputSocket().newInputStream();
                    }
                }
                IOSocket.copy(new ProxyInput(), output);
            } catch (IOException ex) {
                buffer.file.release();
                throw ex;
            }
        }
    } // class OutputBufferPool

    private final class WriteBackOutputBufferPool extends OutputBufferPool {
        @Override
        public void release(final Buffer buffer) throws IOException {
            if (!buffer.dirty) // DCL is OK in this context!
                return;
            synchronized (Cache.this) {
                if (!buffer.dirty)
                    return;
                if (Cache.this.buffer != buffer) {
                    Cache.this.buffer = buffer;
                } else {
                    buffer.dirty = false;
                    super.release(buffer);
                }
            }
        }
    } // class WriteBackOutputBufferPool

    private final class WriteThroughOutputBufferPool extends OutputBufferPool {
        @Override
        public void release(final Buffer buffer) throws IOException {
            if (!buffer.dirty) // DCL is OK in this context!
                return;
            synchronized (Cache.this) {
                if (!buffer.dirty)
                    return;
                if (Cache.this.buffer != buffer && null != Cache.this.buffer && !Cache.this.buffer.dirty && 0 == Cache.this.buffer.reading)
                    Cache.this.buffer.release();
                Cache.this.buffer = buffer;
                buffer.dirty = false;
                super.release(buffer);
            }
        }
    } // class WriteThroughOutputBufferPool

    private final class Buffer {
        final IOPool.Entry<?> file;
        volatile boolean dirty;
        int reading;

        Buffer(final IOPool.Entry<?> file) {
            this.file = file;
        }

        void release() throws IOException {
            file.release();
        }

        final class BufferReadOnlyFile extends DecoratingReadOnlyFile {
            boolean closed;

            BufferReadOnlyFile() throws IOException {
                super(file.getInputSocket().newReadOnlyFile());
            }

            @Override
            public void close() throws IOException {
                if (closed)
                    return;
                closed = true;
                try {
                    delegate.close();
                } finally {
                    getInputBufferPool().release(Buffer.this);
                }
            }
        } // class BufferReadOnlyFile

        final class BufferInputStream extends DecoratingInputStream { // Do NOT extend FileIn|OutputStream: They implement finalize(), which may cause deadlocks!
            boolean closed;

            BufferInputStream() throws IOException {
                super(file.getInputSocket().newInputStream());
            }

            @Override
            public void close() throws IOException {
                if (closed)
                    return;
                closed = true;
                try {
                    delegate.close();
                } finally {
                    getInputBufferPool().release(Buffer.this);
                }
            }
        } // class BufferInputStream

        final class BufferOutputStream extends DecoratingOutputStream { // Do NOT extend FileIn|OutputStream: They implement finalize(), which may cause deadlocks!
            boolean closed;

            BufferOutputStream() throws IOException {
                super(file.getOutputSocket().newOutputStream());
            }

            @Override
            public void close() throws IOException {
                if (closed)
                    return;
                closed = true;
                try {
                    delegate.close();
                } finally {
                    getOutputBufferPool().release(Buffer.this);
                }
            }
        } // class BufferOutputStream
    } // class Buffer

    private final class InputSocketProxy extends DecoratingInputSocket<LT> {
        InputSocketProxy(final InputSocket <? extends LT> input) {
            super(input);
        }

        @Override
        public ReadOnlyFile newReadOnlyFile() throws IOException {
            if (null != getPeerTarget()) {
                // The data for connected sockets cannot not get cached because
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
            return getInputBufferPool().allocate().new BufferReadOnlyFile();
        }

        @Override
        public InputStream newInputStream() throws IOException {
            if (null != getPeerTarget()) {
                // Dito.
                flush();
                return getBoundSocket().newInputStream();
            }
            return getInputBufferPool().allocate().new BufferInputStream();
        }
    } // class InputSocketProxy

    private final class OutputSocketProxy extends DecoratingOutputSocket<LT> {
        OutputSocketProxy(final OutputSocket<? extends LT> output) {
            super(output);
        }

        @Override
        public OutputStream newOutputStream() throws IOException {
            if (null != getPeerTarget()) {
                // Dito, but this time we must clear the cache.
                clear();
                return getBoundSocket().newOutputStream();
            }
            return getOutputBufferPool().allocate().new BufferOutputStream();
        }
    } // class OutputSocketProxy
}
