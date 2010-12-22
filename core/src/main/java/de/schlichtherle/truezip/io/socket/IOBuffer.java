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
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import net.jcip.annotations.ThreadSafe;

/**
 * Implements a buffering strategy for input and output sockets.
 * Using this interface has the following effects:
 * <ul>
 * <li>Upon the first read operation, the contents will be read from the
 *     underlying input socket and temporarily stored in the buffer.
 *     Subsequent or concurrent read operations will be served from the buffer
 *     without re-reading the contents from the underlying input socket again
 *     until the buffer gets {@link InputCache#clear cleared}.
 * <li>At the discretion of the {@link Strategy}, contents written to the
 *     buffer may not be written to the underlying output socket until the
 *     buffer gets {@link OutputCache#flush flushed}.
 * <li>After a write operation, the contents will be temporarily stored in the
 *     buffer for subsequent read operations until the buffer gets
 *     {@link OutputCache#clear cleared}.
 * <li>As a side effect, buffering decouples the underlying storage from its
 *     clients, allowing it to create, read, update or delete its contents
 *     while some clients are still busy on reading or writing the buffered
 *     contents.
 * </ul>
 *
 * @param   <E> The type of the buffered entries.
 * @author  Christian Schlichtherle
 * @version $Id$
 */
@ThreadSafe
public final class IOBuffer<E extends Entry> implements IOCache<E> {

    /** Provides different cache strategies. */
    public enum Strategy {

        /**
         * Any attempt to obtain an output socket will result in a
         * {@link NullPointerException}.
         */
        READ_ONLY {
            @Override <E extends Entry> Pool<IOBuffer<E>.Contents, IOException>
            newOutputContentsPool(IOBuffer<E> cache) {
                throw new AssertionError(); // should throw an NPE before we can get here!
            }
        },

        /**
         * A write-through cache flushes any written data as soon as the
         * output stream created by the provided output socket gets closed.
         */
        WRITE_THROUGH {
            @Override <E extends Entry> Pool<IOBuffer<E>.Contents, IOException>
            newOutputContentsPool(IOBuffer<E> cache) {
                return cache.new WriteThroughOutputContentsPool();
            }
        },

        /**
         * A write-back cache flushes any written data if and only if it gets
         * explicitly flushed.
         */
        WRITE_BACK {
            @Override <E extends Entry> Pool<IOBuffer<E>.Contents, IOException>
            newOutputContentsPool(IOBuffer<E> cache) {
                return cache.new WriteBackOutputContentsPool();
            }
        };

        /** Returns a new input / output cache. */
        @NonNull public <E extends Entry> IOBuffer<E>
        newIOBuffer(Class<E> clazz, IOPool<?> pool) {
            return new IOBuffer<E>(this, pool);
        }

        @NonNull <E extends Entry> Pool<IOBuffer<E>.Contents, IOException>
        newInputContentsPool(IOBuffer<E> cache) {
            return cache.new InputContentsPool();
        }

        @NonNull abstract <E extends Entry> Pool<IOBuffer<E>.Contents, IOException>
        newOutputContentsPool(IOBuffer<E> cache);
    }

    private final Strategy strategy;
    private final IOPool<?> pool;
    private volatile InputSocket<? extends E> input;
    private volatile OutputSocket<? extends E> output;
    private volatile Pool<Contents, IOException> inputContentsPool;
    private volatile Pool<Contents, IOException> outputContentsPool;
    private volatile Contents contents;

    private IOBuffer(@NonNull final Strategy strategy, @NonNull final IOPool<?> pool) {
        if (null == strategy || null == pool)
            throw new NullPointerException();
        this.strategy = strategy;
        this.pool = pool;
    }

    @NonNull
    public IOBuffer<E> configure(@NonNull final InputSocket <? extends E> input) {
        if (null == input)
            throw new NullPointerException();
        this.input = input;
        return this;
    }

    @NonNull
    public IOBuffer<E> configure(@NonNull final OutputSocket <? extends E> output) {
        if (null == output)
            throw new NullPointerException();
        this.output = output;
        return this;
    }

    @Override
    public InputSocket<E> getInputSocket() {
        return new InputSocketProxy(input);
    }

    @Override
    public OutputSocket<E> getOutputSocket() {
        return new OutputSocketProxy(output);
    }

    @Override
    public void flush() throws IOException {
        if (null == contents) // DCL is OK in this context!
            return;
        synchronized (IOBuffer.this) {
            final Contents contents = this.contents;
            if (null != contents)
                getOutputContentsPool().release(contents);
        }
    }

    @Override
    public void clear() throws IOException {
        synchronized (IOBuffer.this) {
            final Contents contents = this.contents;
            this.contents = null;
            if (null != contents && 0 == contents.reading && !contents.dirty)
                contents.release();
        }
    }

    private Pool<Contents, IOException> getInputContentsPool() {
        return null != inputContentsPool
                ? inputContentsPool
                : (inputContentsPool = strategy.newInputContentsPool(this));
    }

    private final class InputContentsPool implements Pool<Contents, IOException> {
        @Override
        public Contents allocate() throws IOException {
            synchronized (IOBuffer.this) {
                Contents contents = IOBuffer.this.contents;
                if (null == contents) {
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
                    IOBuffer.this.contents = contents = new Contents(output.getLocalTarget());
                }
                contents.reading++;
                return contents;
            }
        }

        @Override
        public void release(final Contents contents) throws IOException {
            synchronized (IOBuffer.this) {
                if (0 == --contents.reading && contents != IOBuffer.this.contents && !contents.dirty)
                    contents.release();
            }
        }
    } // class InputContentsPool

    private Pool<Contents, IOException> getOutputContentsPool() {
        return null != outputContentsPool
                ? outputContentsPool
                : (outputContentsPool = strategy.newOutputContentsPool(this));
    }

    private abstract class OutputContentsPool implements Pool<Contents, IOException> {
        @Override
        public Contents allocate() throws IOException {
            final Contents contents = new Contents(pool.allocate());
            contents.dirty = true;
            return contents;
        }

        @Override
        public void release(final Contents contents) throws IOException {
            try {
                assert null == output.getPeerTarget();
                class ProxyInput extends InputSocket<IOPool.Entry<?>> {
                    @Override
                    public IOPool.Entry<?> getLocalTarget() throws IOException {
                        return contents.file;
                    }

                    @Override
                    public ReadOnlyFile newReadOnlyFile() throws IOException {
                        return contents.file.getInputSocket().newReadOnlyFile();
                    }

                    @Override
                    public InputStream newInputStream() throws IOException {
                        return contents.file.getInputSocket().newInputStream();
                    }
                }
                IOSocket.copy(new ProxyInput(), output);
            } catch (IOException ex) {
                contents.file.release();
                throw ex;
            }
        }
    } // class OutputContentsPool

    private final class WriteBackOutputContentsPool extends OutputContentsPool {
        @Override
        public void release(final Contents contents) throws IOException {
            if (!contents.dirty) // DCL is OK in this context!
                return;
            synchronized (IOBuffer.this) {
                if (!contents.dirty)
                    return;
                if (IOBuffer.this.contents != contents) {
                    IOBuffer.this.contents = contents;
                } else {
                    contents.dirty = false;
                    super.release(contents);
                }
            }
        }
    } // class WriteBackOutputContentsPool

    private final class WriteThroughOutputContentsPool extends OutputContentsPool {
        @Override
        public void release(final Contents contents) throws IOException {
            if (!contents.dirty) // DCL is OK in this context!
                return;
            synchronized (IOBuffer.this) {
                if (!contents.dirty)
                    return;
                if (IOBuffer.this.contents != contents && null != IOBuffer.this.contents && !IOBuffer.this.contents.dirty && 0 == IOBuffer.this.contents.reading)
                    IOBuffer.this.contents.release();
                IOBuffer.this.contents = contents;
                contents.dirty = false;
                super.release(contents);
            }
        }
    } // class WriteThroughOutputContentsPool

    private final class Contents {
        final IOPool.Entry<?> file;
        volatile boolean dirty;
        int reading;

        Contents(final IOPool.Entry<?> file) {
            this.file = file;
        }

        void release() throws IOException {
            file.release();
        }

        final class ContentsReadOnlyFile extends DecoratingReadOnlyFile {
            boolean closed;

            ContentsReadOnlyFile() throws IOException {
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
                    getInputContentsPool().release(Contents.this);
                }
            }
        } // class ContentsReadOnlyFile

        final class ContentsInputStream extends DecoratingInputStream { // Do NOT extend FileIn|OutputStream: They implement finalize(), which may cause deadlocks!
            boolean closed;

            ContentsInputStream() throws IOException {
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
                    getInputContentsPool().release(Contents.this);
                }
            }
        } // class ContentsInputStream

        final class ContentsOutputStream extends DecoratingOutputStream { // Do NOT extend FileIn|OutputStream: They implement finalize(), which may cause deadlocks!
            boolean closed;

            ContentsOutputStream() throws IOException {
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
                    getOutputContentsPool().release(Contents.this);
                }
            }
        } // class ContentsOutputStream
    } // class Contents

    private final class InputSocketProxy extends DecoratingInputSocket<E> {
        InputSocketProxy(final InputSocket <? extends E> input) {
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
            return getInputContentsPool().allocate().new ContentsReadOnlyFile();
        }

        @Override
        public InputStream newInputStream() throws IOException {
            if (null != getPeerTarget()) {
                // Dito.
                flush();
                return getBoundSocket().newInputStream();
            }
            return getInputContentsPool().allocate().new ContentsInputStream();
        }
    } // class InputSocketProxy

    private final class OutputSocketProxy extends DecoratingOutputSocket<E> {
        OutputSocketProxy(final OutputSocket<? extends E> output) {
            super(output);
        }

        @Override
        public OutputStream newOutputStream() throws IOException {
            if (null != getPeerTarget()) {
                // Dito, but this time we must clear the cache.
                clear();
                return getBoundSocket().newOutputStream();
            }
            return getOutputContentsPool().allocate().new ContentsOutputStream();
        }
    } // class OutputSocketProxy
}
