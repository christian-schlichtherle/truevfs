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
            @Override <E extends Entry> Pool<IOBuffer<E>.Data, IOException>
            newOutputPool(IOBuffer<E> buffer) {
                throw new AssertionError(); // should throw an NPE before we can get here!
            }
        },

        /**
         * A write-through cache flushes any written data as soon as the
         * output stream created by the provided output socket gets closed.
         */
        WRITE_THROUGH {
            @Override <E extends Entry> Pool<IOBuffer<E>.Data, IOException>
            newOutputPool(IOBuffer<E> buffer) {
                return buffer.new WriteThroughOutputPool();
            }
        },

        /**
         * A write-back cache flushes any written data if and only if it gets
         * explicitly flushed.
         */
        WRITE_BACK {
            @Override <E extends Entry> Pool<IOBuffer<E>.Data, IOException>
            newOutputPool(IOBuffer<E> buffer) {
                return buffer.new WriteBackOutputPool();
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

        @NonNull <E extends Entry> Pool<IOBuffer<E>.Data, IOException>
        newInputPool(IOBuffer<E> buffer) {
            return buffer.new InputPool();
        }

        @NonNull abstract <E extends Entry> Pool<IOBuffer<E>.Data, IOException>
        newOutputPool(IOBuffer<E> buffer);
    }

    private final Strategy strategy;
    private final IOPool<?> pool;
    private volatile InputSocket<? extends E> input;
    private volatile OutputSocket<? extends E> output;
    private volatile Pool<Data, IOException> inputPool;
    private volatile Pool<Data, IOException> outputPool;
    private volatile Data data;

    private IOBuffer(   @NonNull final Strategy strategy,
                        @NonNull final IOPool<?> pool) {
        if (null == strategy || null == pool)
            throw new NullPointerException();
        this.strategy = strategy;
        this.pool = pool;
    }

    // FIXME: Consider calling clear()!
    @NonNull
    public IOBuffer<E> configure(@NonNull final InputSocket <? extends E> input) {
        if (null == input)
            throw new NullPointerException();
        this.input = input;
        return this;
    }

    // FIXME: Consider calling flush()!
    @NonNull
    public IOBuffer<E> configure(@NonNull final OutputSocket <? extends E> output) {
        if (null == output)
            throw new NullPointerException();
        this.output = output;
        return this;
    }

    public InputSocket<E> getInputSocket() {
        return new InputSocketProxy(input);
    }

    public OutputSocket<E> getOutputSocket() {
        return new OutputSocketProxy(output);
    }

    public void flush() throws IOException {
        if (null == data) // DCL is OK in this context!
            return;
        synchronized (IOBuffer.this) {
            final Data data = this.data;
            if (null != data)
                getOutputPool().release(data);
        }
    }

    public void clear() throws IOException {
        synchronized (IOBuffer.this) {
            final Data data = this.data;
            this.data = null;
            if (null != data && 0 == data.reading && !data.dirty)
                data.release();
        }
    }

    private Pool<Data, IOException> getInputPool() {
        return null != inputPool
                ? inputPool
                : (inputPool = strategy.newInputPool(this));
    }

    private final class InputPool implements Pool<Data, IOException> {
        @Override
        public Data allocate() throws IOException {
            synchronized (IOBuffer.this) {
                Data data = IOBuffer.this.data;
                if (null == data) {
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
                    IOBuffer.this.data = data = new Data(output.getLocalTarget());
                }
                data.reading++;
                return data;
            }
        }

        @Override
        public void release(final Data data) throws IOException {
            synchronized (IOBuffer.this) {
                if (0 == --data.reading && data != IOBuffer.this.data && !data.dirty)
                    data.release();
            }
        }
    } // class InputPool

    private Pool<Data, IOException> getOutputPool() {
        return null != outputPool
                ? outputPool
                : (outputPool = strategy.newOutputPool(this));
    }

    private abstract class OutputPool implements Pool<Data, IOException> {
        @Override
        public Data allocate() throws IOException {
            final Data data = new Data(pool.allocate());
            data.dirty = true;
            return data;
        }

        @Override
        public void release(final Data data) throws IOException {
            try {
                assert null == output.getPeerTarget();
                class ProxyInput extends InputSocket<IOPool.Entry<?>> {
                    @Override
                    public IOPool.Entry<?> getLocalTarget() throws IOException {
                        return data.file;
                    }

                    @Override
                    public ReadOnlyFile newReadOnlyFile() throws IOException {
                        return data.file.getInputSocket().newReadOnlyFile();
                    }

                    @Override
                    public InputStream newInputStream() throws IOException {
                        return data.file.getInputSocket().newInputStream();
                    }
                }
                IOSocket.copy(new ProxyInput(), output);
            } catch (IOException ex) {
                data.file.release();
                throw ex;
            }
        }
    } // class OutputPool

    private final class WriteBackOutputPool extends OutputPool {
        @Override
        public void release(final Data data) throws IOException {
            if (!data.dirty) // DCL is OK in this context!
                return;
            synchronized (IOBuffer.this) {
                if (!data.dirty)
                    return;
                if (IOBuffer.this.data != data) {
                    IOBuffer.this.data = data;
                } else {
                    data.dirty = false;
                    super.release(data);
                }
            }
        }
    } // class WriteBackOutputPool

    private final class WriteThroughOutputPool extends OutputPool {
        @Override
        public void release(final Data data) throws IOException {
            if (!data.dirty) // DCL is OK in this context!
                return;
            synchronized (IOBuffer.this) {
                if (!data.dirty)
                    return;
                if (IOBuffer.this.data != data && null != IOBuffer.this.data && !IOBuffer.this.data.dirty && 0 == IOBuffer.this.data.reading)
                    IOBuffer.this.data.release();
                IOBuffer.this.data = data;
                data.dirty = false;
                super.release(data);
            }
        }
    } // class WriteThroughOutputPool

    private final class Data {
        final IOPool.Entry<?> file;
        volatile boolean dirty;
        int reading;

        Data(final IOPool.Entry<?> file) {
            this.file = file;
        }

        void release() throws IOException {
            file.release();
        }

        final class DataReadOnlyFile extends DecoratingReadOnlyFile {
            boolean closed;

            DataReadOnlyFile() throws IOException {
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
                    getInputPool().release(Data.this);
                }
            }
        } // class DataReadOnlyFile

        final class DataInputStream extends DecoratingInputStream { // Do NOT extend FileIn|OutputStream: They implement finalize(), which may cause deadlocks!
            boolean closed;

            DataInputStream() throws IOException {
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
                    getInputPool().release(Data.this);
                }
            }
        } // class DataInputStream

        final class DataOutputStream extends DecoratingOutputStream { // Do NOT extend FileIn|OutputStream: They implement finalize(), which may cause deadlocks!
            boolean closed;

            DataOutputStream() throws IOException {
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
                    getOutputPool().release(Data.this);
                }
            }
        } // class OutputStream
    } // class Data

    private final class InputSocketProxy extends DecoratingInputSocket<E> {
        InputSocketProxy(InputSocket <? extends E> input) {
            super(input);
        }

        @Override
        public ReadOnlyFile newReadOnlyFile() throws IOException {
            /*if (null != getPeerTarget()) {
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
            }*/
            return getInputPool().allocate().new DataReadOnlyFile();
        }

        @Override
        public InputStream newInputStream() throws IOException {
            /*if (null != getPeerTarget()) {
                // Dito.
                flush();
                return getBoundSocket().newInputStream();
            }*/
            return getInputPool().allocate().new DataInputStream();
        }
    } // class InputSocketProxy

    private final class OutputSocketProxy extends DecoratingOutputSocket<E> {
        OutputSocketProxy(OutputSocket<? extends E> output) {
            super(output);
        }

        @Override
        public OutputStream newOutputStream() throws IOException {
            /*if (null != getPeerTarget()) {
                // Dito, but this time we must clear the cache.
                clear();
                return getBoundSocket().newOutputStream();
            }*/
            return getOutputPool().allocate().new DataOutputStream();
        }
    } // class OutputSocketProxy
}
