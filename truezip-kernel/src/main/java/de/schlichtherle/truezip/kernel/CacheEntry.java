/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.kernel;

import static de.truezip.kernel.cio.Entry.UNKNOWN;
import de.truezip.kernel.cio.*;
import de.truezip.kernel.io.DecoratingInputStream;
import de.truezip.kernel.io.DecoratingOutputStream;
import de.truezip.kernel.io.DecoratingReadOnlyChannel;
import de.truezip.kernel.io.DecoratingSeekableChannel;
import de.truezip.kernel.util.Pool;
import de.truezip.kernel.util.Releasable;
import edu.umd.cs.findbugs.annotations.CleanupObligation;
import edu.umd.cs.findbugs.annotations.CreatesObligation;
import edu.umd.cs.findbugs.annotations.DischargesObligation;
import java.io.*;
import java.nio.channels.SeekableByteChannel;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import javax.annotation.concurrent.NotThreadSafe;

/**
 * Provides temporary caching services for input and output sockets with the
 * following features:
 * <ul>
 * <li>Upon the first read operation, the entry data will be read from the
 *     backing store and temporarily stored in the cache.
 *     Subsequent or concurrent read operations will be served from the cache
 *     without re-reading the entry data from the backing store again until
 *     the cache gets {@link #release cleared}.
 * <li>At the discretion of the {@link Strategy}, entry data written to the
 *     cache may not be written to the backing store until the cache gets
 *     {@link #flush flushed}.
 * <li>After a write operation, the entry data will be stored in the cache
 *     for subsequent read operations until the cache gets
 *     {@link #release cleared}.
 * <li>As a side effect, caching decouples the underlying storage from its
 *     clients, allowing it to create, read, update or delete the entry data
 *     while some clients are still busy on reading or writing the cached
 *     entry data.
 * </ul>
 *
 * @author Christian Schlichtherle
 */
@NotThreadSafe
@CleanupObligation
class CacheEntry
implements Entry, Flushable, Releasable<IOException>, Closeable {

    private final Strategy strategy;
    private final IOPool<?> pool;
    private @Nullable InputSocket<?> input;
    private @Nullable OutputSocket<?> output;
    private @CheckForNull InputBufferPool inputBufferPool;
    private @CheckForNull OutputBufferPool outputBufferPool;
    private @CheckForNull Buffer buffer;

    /**
     * Constructs a new cache which applies the given caching strategy,
     * uses the given pool to allocate and release temporary I/O buffers.
     * <p>
     * Note that you need to call {@link #configure(InputSocket)} before
     * you can do any input.
     * Likewise, you need to call {@link #configure(OutputSocket)} before
     * you can do any output.
     *
     * @param strategy the caching strategy.
     * @param pool the pool for allocating and releasing temporary I/O entries.
     */
    private CacheEntry(
            final Strategy strategy,
            final IOPool<?> pool) {
        assert null != strategy;
        this.strategy = strategy;
        if (null == (this.pool = pool))
            throw new NullPointerException();
    }

    /**
     * Configures the input socket for reading the entry data from the
     * backing store.
     * This method needs to be called before any input can be done -
     * otherwise a {@link NullPointerException} will be thrown on the first
     * read attempt.
     * Note that calling this method does <em>not</em> {@link #release() release}
     * this cache.
     *
     * @param  input an input socket for reading the entry data from the
     *         backing store.
     * @return {@code this}
     */
    public CacheEntry configure(final InputSocket<?> input) {
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
     * this cache.
     *
     * @param  output an output socket for writing the entry data to the
     *         backing store.
     * @return {@code this}
     */
    public CacheEntry configure(final OutputSocket<?> output) {
        if (null == output)
            throw new NullPointerException();
        this.output = output;
        return this;
    }

    @Override
    public String getName() {
        return "Johnny Cache!";
    }

    @Override
    public long getSize(Size type) {
        final Buffer buffer = this.buffer;
        return null == buffer ? UNKNOWN : buffer.getSize(type);
    }

    @Override
    public long getTime(Access type) {
        final Buffer buffer = this.buffer;
        return null == buffer ? UNKNOWN : buffer.getTime(type);
    }

    /**
     * Returns an input socket for reading the cached entry data.
     *
     * @return An input socket for reading the cached entry data.
     */
    public InputSocket<Entry> input() {
        return new Input();
    }

    /**
     * Returns an output socket for writing the cached entry data.
     *
     * @return An output socket for writing the cached entry data.
     */
    public OutputSocket<Entry> output() {
        return new Output();
    }

    /**
     * Writes the cached entry data to the backing store unless already done.
     * Whether or not this method needs to be called depends on the caching
     * strategy.
     * E.g. the caching strategy {@link Strategy#WRITE_THROUGH} writes any
     * changed entry data immediately, so calling this method has no effect.
     */
    @Override
    public void flush() throws IOException {
        final Buffer buffer = this.buffer;
        if (null != buffer)
            getOutputBufferPool().release(buffer);
    }

    /**
     * Clears the entry data from this cache without flushing it.
     * 
     * @throws IOException on any I/O error.
     */
    @Override
    public void release() throws IOException {
        setBuffer(null);
    }

    /**
     * {@linkplain #flush() Flushes} and {@linkplain #release() releases}
     * the cached entry data.
     */
    @Override
    @DischargesObligation
    public void close() throws IOException {
        flush();
        release();
    }

    private InputBufferPool getInputBufferPool() {
        InputBufferPool ibp = inputBufferPool;
        return null != ibp
                ? ibp
                : (inputBufferPool = strategy.newInputBufferPool(this));
    }

    private OutputBufferPool getOutputBufferPool() {
        OutputBufferPool obp = this.outputBufferPool;
        return null != obp
                ? obp
                : (outputBufferPool = strategy.newOutputBufferPool(this));
    }

    private void setBuffer(final @CheckForNull Buffer newBuffer)
    throws IOException {
        final Buffer oldBuffer = this.buffer;
        if (oldBuffer != newBuffer) {
            this.buffer = newBuffer;
            if (null != oldBuffer
                    && 0 == oldBuffer.writers
                    && 0 == oldBuffer.readers)
                oldBuffer.release();
        }
    }

    /** Provides different cache strategies. */
    @Immutable
    @SuppressWarnings("PackageVisibleInnerClass")
    enum Strategy {

        /**
         * A write-through cache flushes any written data as soon as the
         * output stream created by {@link #output} gets closed.
         */
        WRITE_THROUGH {
            @Override
            CacheEntry.OutputBufferPool newOutputBufferPool(CacheEntry cache) {
                return cache.new WriteThroughOutputBufferPool();
            }
        },

        /**
         * A write-back cache flushes any written data if and only if it gets
         * explicitly {@link #flush flushed}.
         */
        WRITE_BACK {
            @Override
            CacheEntry.OutputBufferPool newOutputBufferPool(CacheEntry cache) {
                return cache.new WriteBackOutputBufferPool();
            }
        };

        /**
         * Returns a new cache.
         *
         * @param  pool the pool of temporary entries to cache the entry data.
         * @return A new cache.
         */
        @CreatesObligation
        public CacheEntry newCache(IOPool<?> pool) {
            return new CacheEntry(this, pool);
        }

        CacheEntry.InputBufferPool newInputBufferPool(CacheEntry cache) {
            return cache.new InputBufferPool();
        }

        abstract CacheEntry.OutputBufferPool newOutputBufferPool(CacheEntry cache);
    } // Strategy

    private final class Input extends InputSocket<Entry> {
        @CheckForNull Buffer buffer;

        InputSocket<? extends Entry> getSocket() throws IOException {
            return (buffer = getInputBufferPool().allocate()).input();
        }

        InputSocket<? extends Entry> getBoundSocket() throws IOException {
            return getSocket().bind(this);
        }

        @Override
        public Entry localTarget() throws IOException {
            final Buffer buffer = this.buffer;
            return null != buffer ? buffer : new ProxyEntry(
                    input/*.bind(this)*/.localTarget()); // do NOT bind!
        }

        @Override
        public InputStream stream() throws IOException {
            return getBoundSocket().stream();
        }

        @Override
        public SeekableByteChannel channel() throws IOException {
            return getBoundSocket().channel();
        }
    } // Input

    private final class Output extends OutputSocket<Entry> {
        @CheckForNull Buffer buffer;

        OutputSocket<? extends Entry> getSocket() throws IOException {
            return (buffer = getOutputBufferPool().allocate()).output();
        }

        OutputSocket<? extends Entry> getBoundSocket() throws IOException {
            return getSocket().bind(this);
        }

        @Override
        public Entry localTarget() throws IOException {
            final Buffer buffer = this.buffer;
            return null != buffer ? buffer : new ProxyEntry(
                    output/*.bind(this)*/.localTarget()); // do NOT bind!
        }

        @Override
        public OutputStream stream() throws IOException {
            return getBoundSocket().stream();
        }

        @Override
        public SeekableByteChannel channel() throws IOException {
            return getBoundSocket().channel();
        }
    } // Output

    /** Used to proxy the backing store entries. */
    @Immutable
    private static final class ProxyEntry extends DecoratingEntry<Entry> {
        ProxyEntry(Entry entry) {
            super(entry);
        }
    } // ProxyEntry

    @Immutable
    private final class InputBufferPool
    implements Pool<Buffer, IOException> {
        @Override
        public Buffer allocate() throws IOException {
            Buffer buffer = CacheEntry.this.buffer;
            if (null == buffer) {
                buffer = new Buffer();
                try {
                    buffer.load(input);
                } catch (final Throwable ex) {
                    try {
                        buffer.release();
                    } catch (final IOException ex2) {
                        ex.addSuppressed(ex2);
                    }
                    throw ex;
                }
                setBuffer(buffer);
            }
            assert Strategy.WRITE_BACK == strategy || 0 == buffer.writers;
            buffer.readers++;
            return buffer;
        }

        @Override
        public void release(final Buffer buffer) throws IOException {
            assert Strategy.WRITE_BACK == strategy || 0 == buffer.writers;
            if (0 == --buffer.readers && 0 == buffer.writers
                    && CacheEntry.this.buffer != buffer) {
                buffer.release();
            }
        }
    } // InputBufferPool

    @Immutable
    private abstract class OutputBufferPool
    implements Pool<Buffer, IOException> {
        @Override
        public Buffer allocate() throws IOException {
            final Buffer buffer = new Buffer();
            assert 0 == buffer.readers;
            buffer.writers = 1;
            return buffer;
        }

        @Override
        public void release(final Buffer buffer) throws IOException {
            assert Strategy.WRITE_BACK == strategy || 0 == buffer.readers;
            buffer.writers = 0;
            try {
                buffer.save(output);
            } finally {
                setBuffer(buffer);
            }
        }
    } // OutputBufferPool

    @Immutable
    private final class WriteThroughOutputBufferPool extends OutputBufferPool {
        @Override
        public void release(Buffer buffer) throws IOException {
            if (0 != buffer.writers)
                super.release(buffer);
        }
    } // WriteThroughOutputBufferPool

    @Immutable
    private final class WriteBackOutputBufferPool extends OutputBufferPool {
        @Override
        public void release(final Buffer buffer) throws IOException {
            if (0 != buffer.writers)
                if (CacheEntry.this.buffer != buffer)
                    setBuffer(buffer);
                else
                    super.release(buffer);
        }
    } // WriteBackOutputBufferPool

    /** An I/O buffer with the cached contents. */
    private final class Buffer implements IOBuffer<Buffer> {
        final IOBuffer<?> data;

        int readers, writers; // max one writer!

        Buffer() throws IOException {
            data = pool.allocate();
        }

        @Override
        public String getName() {
            return data.getName();
        }

        @Override
        public long getSize(Size type) {
            return data.getSize(type);
        }

        @Override
        public long getTime(Access type) {
            return data.getTime(type);
        }

        @Override
        public InputSocket<Buffer> input() {
            return new Input();
        }

        @Override
        public OutputSocket<Buffer> output() {
            return new Output();
        }

        void load(InputSocket<?> input) throws IOException {
            IOSocket.copy(input, data.output());
        }

        void save(OutputSocket<?> output) throws IOException {
            IOSocket.copy(data.input(), output);
        }

        @Override
        public void release() throws IOException {
            assert 0 == writers;
            assert 0 == readers;
            data.release();
        }

        @NotThreadSafe
        final class Input extends InputSocket<Buffer> {
            final InputSocket<?> socket = data.input();

            InputSocket<?> getBoundSocket() {
                return socket.bind(this);
            }

            @Override
            public Buffer localTarget() throws IOException {
                return Buffer.this;
            }

            @Override
            public InputStream stream() throws IOException {
                return new Stream();
            }

            final class Stream extends DecoratingInputStream {
                boolean closed;

                Stream() throws IOException {
                    super(getBoundSocket().stream());
                }

                @Override
                @DischargesObligation
                public void close() throws IOException {
                    if (closed)
                        return;
                    // HC SUNT DRACONES!
                    in.close();
                    getInputBufferPool().release(Buffer.this);
                    closed = true;
                }
            } // Stream

            @Override
            public SeekableByteChannel channel() throws IOException {
                return new Channel();
            }

            final class Channel extends DecoratingReadOnlyChannel {
                boolean closed;

                Channel() throws IOException {
                    super(getBoundSocket().channel());
                }

                @Override
                @DischargesObligation
                public void close() throws IOException {
                    if (closed)
                        return;
                    // HC SUNT DRACONES!
                    channel.close();
                    getInputBufferPool().release(Buffer.this);
                    closed = true;
                }
            } // Channel
        } // Input

        @NotThreadSafe
        final class Output extends OutputSocket<Buffer> {
            final OutputSocket<?> socket = data.output();

            OutputSocket<?> getBoundSocket() {
                return socket.bind(this);
            }

            @Override
            public Buffer localTarget() throws IOException {
                return Buffer.this;
            }

            @Override
            public OutputStream stream() throws IOException {
                return new Stream();
            }

            final class Stream extends DecoratingOutputStream {
                boolean closed;

                Stream() throws IOException {
                    super(getBoundSocket().stream());
                }

                @Override
                @DischargesObligation
                public void close() throws IOException {
                    if (closed)
                        return;
                    // HC SUNT DRACONES!
                    out.close();
                    getOutputBufferPool().release(Buffer.this);
                    closed = true;
                }
            } // Stream

            @Override
            public SeekableByteChannel channel() throws IOException {
                return new Channel();
            }

            final class Channel extends DecoratingSeekableChannel {
                boolean closed;

                Channel() throws IOException {
                    super(getBoundSocket().channel());
                }

                @Override
                @DischargesObligation
                public void close() throws IOException {
                    if (closed)
                        return;
                    // HC SUNT DRACONES!
                    channel.close();
                    getOutputBufferPool().release(Buffer.this);
                    closed = true;
                }
            } // Channel
        } // Output
    } // IOBuffer
}
