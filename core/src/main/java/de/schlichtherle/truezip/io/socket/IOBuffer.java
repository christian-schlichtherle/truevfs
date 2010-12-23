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

import de.schlichtherle.truezip.io.DecoratingInputStream;
import de.schlichtherle.truezip.io.DecoratingOutputStream;
import de.schlichtherle.truezip.io.entry.Entry;
import de.schlichtherle.truezip.io.rof.DecoratingReadOnlyFile;
import de.schlichtherle.truezip.io.rof.ReadOnlyFile;
import de.schlichtherle.truezip.util.Pool;
import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import net.jcip.annotations.ThreadSafe;

/**
 * Implements a buffering strategy for input and output sockets.
 * Using this interface has the following effects:
 * <ul>
 * <li>Upon the first read operation, the data will be read from the
 *     underlying input socket and temporarily stored in the buffer.
 *     Subsequent or concurrent read operations will be served from the buffer
 *     without re-reading the data from the underlying input socket again
 *     until the buffer gets {@link #clear cleared}.
 * <li>At the discretion of the {@link Strategy}, data written to the
 *     buffer may not be written to the underlying output socket until the
 *     buffer gets {@link #flush flushed}.
 * <li>After a write operation, the data will be temporarily stored in the
 *     buffer for subsequent read operations until the buffer gets
 *     {@link #clear cleared}.
 * <li>As a side effect, buffering decouples the underlying storage from its
 *     clients, allowing it to create, read, update or delete its data
 *     while some clients are still busy on reading or writing the buffered
 *     data.
 * </ul>
 *
 * @param   <E> The type of the buffered entries.
 * @author  Christian Schlichtherle
 * @version $Id$
 */
@ThreadSafe
public final class IOBuffer<E extends Entry> {

    /** Provides different cache strategies. */
    public enum Strategy {

        /**
         * Any attempt to obtain an output socket will result in a
         * {@link NullPointerException}.
         */
        READ_ONLY {
            @Override <E extends Entry> IOBuffer<E>.OutputPool
            newOutputPool(IOBuffer<E> ioBuffer) {
                throw new AssertionError(); // should throw an NPE before we can get here!
            }
        },

        /**
         * A write-through cache flushes any written data as soon as the
         * output stream created by the provided output socket gets closed.
         */
        WRITE_THROUGH {
            @Override <E extends Entry> IOBuffer<E>.OutputPool
            newOutputPool(IOBuffer<E> ioBuffer) {
                return ioBuffer.new WriteThroughOutputPool();
            }
        },

        /**
         * A write-back cache flushes any written data if and only if it gets
         * explicitly flushed.
         */
        WRITE_BACK {
            @Override <E extends Entry> IOBuffer<E>.OutputPool
            newOutputPool(IOBuffer<E> ioBuffer) {
                return ioBuffer.new WriteBackOutputPool();
            }
        };

        /**
         * Returns a new I/O buffer.
         *
         * @param  clazz the class indicating the type of entries for which this
         *         I/O buffer is going to be used.
         *         This is only required to infer the type parameter of the
         *         returned object.
         * @param  pool the pool of temporary entries to buffer the entry
         *         data.
         * @return A new I/O buffer.
         */
        @NonNull public <E extends Entry> IOBuffer<E>
        newIOBuffer(@Nullable Class<E> clazz, @NonNull IOPool<?> pool) {
            return new IOBuffer<E>(this, pool);
        }

        @NonNull <E extends Entry> IOBuffer<E>.InputPool
        newInputPool(IOBuffer<E> ioBuffer) {
            return ioBuffer.new InputPool();
        }

        @NonNull abstract <E extends Entry> IOBuffer<E>.OutputPool
        newOutputPool(IOBuffer<E> ioBuffer);
    }

    private final Strategy strategy;
    private final IOPool<?> pool;
    private volatile InputSocket<? extends E> input;
    private volatile OutputSocket<? extends E> output;
    private volatile InputPool inputPool;
    private volatile OutputPool outputPool;
    private volatile Buffer buffer;

    /**
     * Constructs a new I/O buffer which applies the given buffering strategy
     * and uses the given pool to allocate and release temporary I/O entries.
     * <p>
     * Note that you need to call {@link #configure(InputSocket)} before
     * you can do any input.
     * Likewise, you need to call {@link #configure(OutputSocket)} before
     * you can do any output.
     *
     * @param strategy the buffering strategy.
     * @param pool the pool for allocating and releasing temporary I/O entries.
     */
    private IOBuffer(   @NonNull final Strategy strategy,
                        @NonNull final IOPool<?> pool) {
        if (null == strategy || null == pool)
            throw new NullPointerException();
        this.strategy = strategy;
        this.pool = pool;
    }

    /**
     * Configures the input socket for reading the entry data from the
     * backing store.
     * This method needs to be called before any input can be done -
     * otherwise a {@link NullPointerException} will be thrown on the first
     * read attempt.
     * Note that calling this method does <em>not</em> {@link #clear() clear}
     * this buffer.
     *
     * @param input an input socket for reading the entry data from the
     *        backing store.
     * @return this
     */
    @NonNull
    public IOBuffer<E> configure(@NonNull final InputSocket <? extends E> input) {
        if (null == input)
            throw new NullPointerException();
        this.input = input;
        return this;
    }

    /**
     * Configures the output socket for writing the entry data to the
     * backing store.
     * This method needs to be called before any output can be done -
     * otherwise a {@link NullPointerException} will be thrown on the first
     * write attempt.
     * Note that calling this method does <em>not</em> {@link #flush() flush}
     * this buffer.
     *
     * @param output an output socket for writing the entry data to the
     *        backing store.
     * @return this
     */
    @NonNull
    public IOBuffer<E> configure(@NonNull final OutputSocket <? extends E> output) {
        if (null == output)
            throw new NullPointerException();
        this.output = output;
        return this;
    }

    /**
     * Writes the buffered entry data to the backing store unless already done.
     * Whether or not this method needs to be called depends on the buffering
     * strategy.
     * E.g. the buffering strategy {@link Strategy#WRITE_THROUGH} writes any
     * changed entry data immediately, so calling this method has no effect.
     *
     * @return this
     */
    public IOBuffer<E> flush() throws IOException {
        if (null == getBuffer()) // DCL is OK in this context!
            return this;
        synchronized (IOBuffer.this) {
            final Buffer buffer = getBuffer();
            if (null != buffer)
                getOutputPool().release(buffer);
        }
        return this;
    }

    /**
     * Discards the entry data in this buffer.
     *
     * @return this
     */
    public IOBuffer<E> clear() throws IOException {
        synchronized (IOBuffer.this) {
            setBuffer(null);
        }
        return this;
    }

    /**
     * Returns an input socket for reading the buffered entry data.
     *
     * @return An input socket for reading the buffered entry data.
     */
    @NonNull
    public InputSocket<E> getInputSocket() {
        return new BufferInputSocket(input);
    }

    /**
     * Returns an output socket for writing the buffered entry data.
     *
     * @return An output socket for writing the buffered entry data.
     */
    @NonNull
    public OutputSocket<E> getOutputSocket() {
        return new BufferOutputSocket(output);
    }

    private Buffer getBuffer() {
        return buffer;
    }

    private void setBuffer(final Buffer newBuffer) throws IOException {
        final Buffer oldBuffer = this.buffer;
        if (oldBuffer != newBuffer) {
            this.buffer = newBuffer;
            if (oldBuffer != null
                    && oldBuffer.writers == 0
                    && oldBuffer.readers == 0)
                oldBuffer.release();
        }
    }

    @Override
    @SuppressWarnings("FinalizeDeclaration")
    protected void finalize() throws Throwable {
        try {
            setBuffer(null);
        } finally {
            super.finalize();
        }
    }

    private InputPool getInputPool() {
        return null != inputPool
                ? inputPool : (inputPool = strategy.newInputPool(this));
    }

    private final class InputPool implements Pool<Buffer, IOException> {
        ReadOnlyFile newReadOnlyFile(InputSocket<?> input) throws IOException {
            return allocate().newReadOnlyFile(input);
        }

        InputStream newInputStream(InputSocket<?> input) throws IOException {
            return allocate().newInputStream(input);
        }

        @Override
        public Buffer allocate() throws IOException {
            synchronized (IOBuffer.this) {
                Buffer buffer = getBuffer();
                if (null == buffer) {
                    buffer = new Buffer();
                    try {
                        IOSocket.copy(input, buffer.getOutputSocket());
                    } catch (IOException ex) {
                        buffer.release();
                        throw ex;
                    }
                    setBuffer(buffer);
                }
                buffer.readers++;
                return buffer;
            }
        }

        @Override
        public void release(final Buffer buffer) throws IOException {
            synchronized (IOBuffer.this) {
                if (--buffer.readers == 0 && buffer.writers == 0 && buffer != getBuffer())
                    buffer.release();
            }
        }
    } // class InputPool

    private OutputPool getOutputPool() {
        return null != outputPool
                ? outputPool : (outputPool = strategy.newOutputPool(this));
    }

    private abstract class OutputPool implements Pool<Buffer, IOException> {
        OutputStream newOutputStream(OutputSocket<?> output) throws IOException {
            return allocate().newOutputStream(output);
        }

        @Override
        public Buffer allocate() throws IOException {
            final Buffer buffer = new Buffer();
            buffer.writers = 1;
            return buffer;
        }

        @Override
        public void release(final Buffer buffer) throws IOException {
            buffer.writers = 0;
            try {
                IOSocket.copy(buffer.getInputSocket(), output);
            } finally {
                setBuffer(buffer);
            }
        }
    } // class OutputPool

    private class WriteThroughOutputPool extends OutputPool {
        @Override
        public void release(Buffer buffer) throws IOException {
            if (buffer.writers == 0) // DCL is OK in this context!
                return;
            synchronized (IOBuffer.this) {
                if (buffer.writers == 0)
                    return;
                super.release(buffer);
            }
        }
    } // class WriteThroughOutputPool

    private final class WriteBackOutputPool extends OutputPool {
        @Override
        public void release(final Buffer buffer) throws IOException {
            if (buffer.writers == 0) // DCL is OK in this context!
                return;
            synchronized (IOBuffer.this) {
                if (buffer.writers == 0)
                    return;
                if (getBuffer() != buffer) {
                    setBuffer(buffer);
                } else {
                    super.release(buffer);
                }
            }
        }
    } // class WriteBackOutputPool

    private final class Buffer {
        private final IOPool.Entry<?> data;
        volatile int readers, writers; // max one writer!

        Buffer() throws IOException {
            data = pool.allocate();
        }

        InputSocket<?> getInputSocket() {
            return data.getInputSocket();
        }

        OutputSocket<?> getOutputSocket() {
            return data.getOutputSocket();
        }

        void release() throws IOException {
            assert 0 == writers;
            assert 0 == readers;
            data.release();
        }

        ReadOnlyFile newReadOnlyFile(InputSocket<?> input) throws IOException {
            return new BufferReadOnlyFile(input);
        }

        private final class BufferReadOnlyFile extends DecoratingReadOnlyFile {
            private boolean closed;

            BufferReadOnlyFile(InputSocket<?> input) throws IOException {
                super(getInputSocket().bind(input).newReadOnlyFile());
            }

            @Override
            public void close() throws IOException {
                if (closed)
                    return;
                closed = true;
                try {
                    delegate.close();
                } finally {
                    getInputPool().release(Buffer.this);
                }
            }
        } // class BufferReadOnlyFile

        InputStream newInputStream(InputSocket<?> input) throws IOException {
            return new BufferInputStream(input);
        }

        private final class BufferInputStream extends DecoratingInputStream {
            private boolean closed;

            BufferInputStream(InputSocket<?> input) throws IOException {
                super(getInputSocket().bind(input).newInputStream());
            }

            @Override
            public void close() throws IOException {
                if (closed)
                    return;
                closed = true;
                try {
                    delegate.close();
                } finally {
                    getInputPool().release(Buffer.this);
                }
            }
        } // class BufferInputStream

        OutputStream newOutputStream(OutputSocket<?> output) throws IOException {
            return new BufferOutputStream(output);
        }

        private final class BufferOutputStream extends DecoratingOutputStream {
            private boolean closed;

            BufferOutputStream(OutputSocket<?> output) throws IOException {
                super(getOutputSocket().bind(output).newOutputStream());
            }

            @Override
            public void close() throws IOException {
                if (closed)
                    return;
                closed = true;
                try {
                    delegate.close();
                } finally {
                    getOutputPool().release(Buffer.this);
                }
            }
        } // class BufferOutputStream
    } // class Buffer

    private final class BufferInputSocket extends DecoratingInputSocket<E> {
        BufferInputSocket(InputSocket <? extends E> input) {
            super(input);
        }

        @Override
        public ReadOnlyFile newReadOnlyFile() throws IOException {
            return getInputPool().newReadOnlyFile(this);
        }

        @Override
        public InputStream newInputStream() throws IOException {
            return getInputPool().newInputStream(this);
        }
    } // class BufferInputSocket

    private final class BufferOutputSocket extends DecoratingOutputSocket<E> {
        BufferOutputSocket(OutputSocket<? extends E> output) {
            super(output);
        }

        @Override
        public OutputStream newOutputStream() throws IOException {
            return getOutputPool().newOutputStream(this);
        }
    } // class BufferOutputSocket
}
