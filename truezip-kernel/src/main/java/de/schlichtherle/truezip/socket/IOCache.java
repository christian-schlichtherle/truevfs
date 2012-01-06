/*
 * Copyright 2004-2012 Schlichtherle IT Services
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package de.schlichtherle.truezip.socket;

import de.schlichtherle.truezip.io.DecoratingInputStream;
import de.schlichtherle.truezip.io.DecoratingOutputStream;
import de.schlichtherle.truezip.entry.Entry;
import de.schlichtherle.truezip.entry.DecoratingEntry;
import de.schlichtherle.truezip.io.DecoratingSeekableByteChannel;
import de.schlichtherle.truezip.rof.DecoratingReadOnlyFile;
import de.schlichtherle.truezip.rof.ReadOnlyFile;
import de.schlichtherle.truezip.util.JSE7;
import de.schlichtherle.truezip.util.Pool;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.DefaultAnnotation;
import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import java.io.Closeable;
import java.io.Flushable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.SeekableByteChannel;
import net.jcip.annotations.Immutable;
import net.jcip.annotations.ThreadSafe;

/**
 * Provides temporary caching services for input and output sockets with the
 * following features:
 * <ul>
 * <li>Upon the first read operation, the entry data will be read from the
 *     backing store and temporarily stored in the cache.
 *     Subsequent or concurrent read operations will be served from the cache
 *     without re-reading the entry data from the backing store again until
 *     the cache gets {@link #clear cleared}.
 * <li>At the discretion of the {@link Strategy}, entry data written to the
 *     cache may not be written to the backing store until the cache gets
 *     {@link #flush flushed}.
 * <li>After a write operation, the entry data will be stored in the cache
 *     for subsequent read operations until the cache gets
 *     {@link #clear cleared}.
 * <li>As a side effect, caching decouples the underlying storage from its
 *     clients, allowing it to create, read, update or delete the entry data
 *     while some clients are still busy on reading or writing the cached
 *     entry data.
 * <li>When a cache gets picked up by the finalizer thread of the JVM, it gets
 *     cleared (this class is not a persistence service).
 * </ul>
 *
 * @author  Christian Schlichtherle
 * @version $Id$
 */
@ThreadSafe
@DefaultAnnotation(NonNull.class)
public final class IOCache implements Flushable, Closeable {

    private static final BufferSocketFactory FACTORY = JSE7.AVAILABLE
            ? BufferSocketFactory.NIO2
            : BufferSocketFactory.OIO;

    private final Lock lock = new Lock();
    private final Strategy strategy;
    private final IOPool<?> pool;
    private volatile @Nullable InputSocket<?> input;
    private volatile @Nullable OutputSocket<?> output;
    private volatile @CheckForNull InputBufferPool inputBufferPool;
    private volatile @CheckForNull OutputBufferPool outputBufferPool;
    private volatile @CheckForNull Buffer buffer;

    /**
     * Constructs a new cache which applies the given caching strategy
     * and uses the given pool to allocate and release temporary I/O entries.
     * <p>
     * Note that you need to call {@link #configure(InputSocket)} before
     * you can do any input.
     * Likewise, you need to call {@link #configure(OutputSocket)} before
     * you can do any output.
     *
     * @param strategy the caching strategy.
     * @param pool the pool for allocating and releasing temporary I/O entries.
     */
    private IOCache(final Strategy strategy,
                    final IOPool<?> pool) {
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
     * this cache.
     *
     * @param  input an input socket for reading the entry data from the
     *         backing store.
     * @return {@code this}
     */
    public IOCache configure(final InputSocket<?> input) {
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
    public IOCache configure(final OutputSocket<?> output) {
        if (null == output)
            throw new NullPointerException();
        this.output = output;
        return this;
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
        if (null == getBuffer()) // DCL does work with volatile fields since JSE 5!
            return;
        synchronized (lock) {
            final Buffer buffer = getBuffer();
            if (null != buffer)
                getOutputBufferPool().release(buffer);
        }
    }

    /**
     * Discards the entry data in this buffer.
     */
    public void clear() throws IOException {
        synchronized (lock) {
            setBuffer(null);
        }
    }

    /**
     * {@link #flush() Flushes} and finally {@link #clear() clears} this cache.
     */
    @Override
    public void close() throws IOException {
        synchronized (lock) {
            try {
                final Buffer buffer = getBuffer();
                if (null != buffer)
                    getOutputBufferPool().release(buffer);
            } finally {
                setBuffer(null);
            }
        }
    }

    public @Nullable Entry getEntry() {
        Buffer buffer = getBuffer();
        return null == buffer ? null : buffer.data;
    }

    /**
     * Returns an input socket for reading the cached entry data.
     *
     * @return An input socket for reading the cached entry data.
     */
    public InputSocket<?> getInputSocket() {
        return new Input();
    }

    /**
     * Returns an output socket for writing the cached entry data.
     *
     * @return An output socket for writing the cached entry data.
     */
    public OutputSocket<?> getOutputSocket() {
        return new Output();
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

    private @CheckForNull Buffer getBuffer() {
        return buffer;
    }

    private void setBuffer(final @CheckForNull Buffer newBuffer)
    throws IOException {
        final Buffer oldBuffer = this.buffer;
        if (oldBuffer != newBuffer) {
            this.buffer = newBuffer;
            if (null != oldBuffer
                    && oldBuffer.writers == 0
                    && oldBuffer.readers == 0)
                oldBuffer.release();
        }
    }

    /** Provides different cache strategies. */
    @Immutable
    public enum Strategy {

        /**
         * Any attempt to obtain an output socket will result in a
         * {@link NullPointerException}.
         */
        READ_ONLY {
            @Override IOCache.OutputBufferPool
            newOutputBufferPool(IOCache cache) {
                throw new AssertionError(); // should throw an NPE before we can get here!
            }
        },

        /**
         * A write-through cache flushes any written data as soon as the
         * output stream created by {@link #getOutputSocket} gets closed.
         */
        WRITE_THROUGH {
            @Override IOCache.OutputBufferPool
            newOutputBufferPool(IOCache cache) {
                return cache.new WriteThroughOutputBufferPool();
            }
        },

        /**
         * A write-back cache flushes any written data if and only if it gets
         * explicitly {@link #flush flushed}.
         */
        WRITE_BACK {
            @Override IOCache.OutputBufferPool
            newOutputBufferPool(IOCache cache) {
                return cache.new WriteBackOutputBufferPool();
            }
        };

        /**
         * Returns a new cache.
         *
         * @param  pool the pool of temporary entries to cache the entry data.
         * @return A new cache.
         */
        public IOCache
        newCache(IOPool<?> pool) {
            return new IOCache(this, pool);
        }

        IOCache.InputBufferPool
        newInputBufferPool(IOCache cache) {
            return cache.new InputBufferPool();
        }

        abstract IOCache.OutputBufferPool
        newOutputBufferPool(IOCache cache);
    } // Strategy

    private static final class Lock { }

    private final class Input extends DelegatingInputSocket<Entry> {
        volatile @CheckForNull Buffer buffer;

        @Override
        protected InputSocket<? extends Entry> getDelegate() throws IOException {
            return (buffer = getInputBufferPool().allocate()).getInputSocket();
        }

        @Override
        protected InputSocket<? extends Entry> getBoundSocket() throws IOException {
            // Same implementation as super class, but makes stack trace nicer.
            return getDelegate().bind(this);
        }

        @Override
        public Entry getLocalTarget() throws IOException {
            final Buffer buffer = this.buffer;
            return null != buffer
                    ? buffer.data
                    : new CacheEntry(input.getLocalTarget());
        }

        @Override
        public Entry getPeerTarget() throws IOException {
            // Same implementation as super class, but makes stack trace nicer.
            return getBoundSocket().getPeerTarget();
        }

        @Override
        public SeekableByteChannel newSeekableByteChannel() throws IOException {
            // Same implementation as super class, but makes stack trace nicer.
            return getBoundSocket().newSeekableByteChannel();
        }

        @Override
        public ReadOnlyFile newReadOnlyFile() throws IOException {
            // Same implementation as super class, but makes stack trace nicer.
            return getBoundSocket().newReadOnlyFile();
        }

        @Override
        public InputStream newInputStream() throws IOException {
            // Same implementation as super class, but makes stack trace nicer.
            return getBoundSocket().newInputStream();
        }
    } // Input

    private final class Output extends DelegatingOutputSocket<Entry> {
        volatile @CheckForNull Buffer buffer;

        @Override
        protected OutputSocket<? extends Entry> getDelegate() throws IOException {
            return (buffer = getOutputBufferPool().allocate()).getOutputSocket();
        }

        @Override
        protected OutputSocket<? extends Entry> getBoundSocket() throws IOException {
            // Same implementation as super class, but makes stack trace nicer.
            return getDelegate().bind(this);
        }

        @Override
        public Entry getLocalTarget() throws IOException {
            final Buffer buffer = this.buffer;
            return null != buffer
                    ? buffer.data
                    : new CacheEntry(output.getLocalTarget());
        }

        @Override
        public Entry getPeerTarget() throws IOException {
            // Same implementation as super class, but makes stack trace nicer.
            return getBoundSocket().getPeerTarget();
        }

        @Override
        public SeekableByteChannel newSeekableByteChannel() throws IOException {
            // Same implementation as super class, but makes stack trace nicer.
            return getBoundSocket().newSeekableByteChannel();
        }

        @Override
        public OutputStream newOutputStream() throws IOException {
            // Same implementation as super class, but makes stack trace nicer.
            return getBoundSocket().newOutputStream();
        }
    } // Output

    /** Hides backing store entries. */
    @Immutable
    private static class CacheEntry extends DecoratingEntry<Entry> {
        CacheEntry(Entry entry) {
            super(entry);
        }
    } // CacheEntry

    @Immutable
    private final class InputBufferPool implements Pool<Buffer, IOException> {
        @Override
        public Buffer allocate() throws IOException {
            synchronized (lock) {
                Buffer buffer = getBuffer();
                if (null == buffer) {
                    buffer = new Buffer();
                    try {
                        IOSocket.copy(input, buffer.data.getOutputSocket());
                    } catch (IOException ex) {
                        buffer.release();
                        throw ex;
                    }
                    setBuffer(buffer);
                }
                assert Strategy.WRITE_BACK == strategy || 0 == buffer.writers;
                buffer.readers++;
                return buffer;
            }
        }

        @Override
        public void release(final Buffer buffer) throws IOException {
            synchronized (lock) {
                assert Strategy.WRITE_BACK == strategy || 0 == buffer.writers;
                if (0 == --buffer.readers && 0 == buffer.writers && getBuffer() != buffer)
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
                IOSocket.copy(buffer.data.getInputSocket(), output);
            } finally {
                setBuffer(buffer);
            }
        }
    } // OutputBufferPool

    @Immutable
    private final class WriteThroughOutputBufferPool extends OutputBufferPool {
        @Override
        public void release(Buffer buffer) throws IOException {
            if (0 == buffer.writers) // DCL does work with volatile fields since JSE 5!
                return;
            synchronized (lock) {
                if (0 == buffer.writers)
                    return;
                super.release(buffer);
            }
        }
    } // WriteThroughOutputBufferPool

    @Immutable
    private final class WriteBackOutputBufferPool extends OutputBufferPool {
        @Override
        public void release(final Buffer buffer) throws IOException {
            if (0 == buffer.writers) // DCL does work with volatile fields since JSE 5!
                return;
            synchronized (lock) {
                if (0 == buffer.writers)
                    return;
                if (getBuffer() != buffer) {
                    setBuffer(buffer);
                } else {
                    super.release(buffer);
                }
            }
        }
    } // WriteBackOutputBufferPool

    @Immutable
    private enum BufferSocketFactory {
        OIO() {
            @Override
            InputSocket<?> newInputSocket(Buffer buffer) {
                return buffer.new BufferInputSocket();
            }

            @Override
            OutputSocket<?> newOutputSocket(Buffer buffer) {
                return buffer.new BufferOutputSocket();
            }
        },

        NIO2() {
            @Override
            InputSocket<?> newInputSocket(Buffer buffer) {
                return buffer.new Nio2BufferInputSocket();
            }

            @Override
            OutputSocket<?> newOutputSocket(Buffer buffer) {
                return buffer.new Nio2BufferOutputSocket();
            }
        };
        
        abstract InputSocket<?> newInputSocket(Buffer buffer);
        abstract OutputSocket<?> newOutputSocket(Buffer buffer);
    } // BufferSocketFactory

    private final class Buffer {
        final IOPool.Entry<?> data;

        volatile int readers, writers; // max one writer!

        Buffer() throws IOException {
            data = pool.allocate();
        }

        InputSocket<?> getInputSocket() {
            return FACTORY.newInputSocket(this);
        }

        OutputSocket<?> getOutputSocket() {
            return FACTORY.newOutputSocket(this);
        }

        void release() throws IOException {
            assert 0 == writers;
            assert 0 == readers;
            data.release();
        }

        @Immutable
        final class Nio2BufferInputSocket extends BufferInputSocket {
            @Override
            public SeekableByteChannel newSeekableByteChannel() throws IOException {
                return new BufferInputChannel(getBoundSocket().newSeekableByteChannel());
            }
        } // Nio2BufferInputSocket

        @Immutable
        class BufferInputSocket extends DecoratingInputSocket<Entry> {
            BufferInputSocket() {
                super(data.getInputSocket());
            }

            @Override
            public final ReadOnlyFile newReadOnlyFile() throws IOException {
                return new BufferReadOnlyFile(getBoundSocket().newReadOnlyFile());
            }

            @Override
            public final InputStream newInputStream() throws IOException {
                return new BufferInputStream(getBoundSocket().newInputStream());
            }
        } // BufferInputSocket

        final class BufferInputChannel extends DecoratingSeekableByteChannel {
            boolean closed;

            BufferInputChannel(SeekableByteChannel sbc) {
                super(sbc);
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
        } // BufferInputChannel

        final class BufferReadOnlyFile extends DecoratingReadOnlyFile {
            boolean closed;

            BufferReadOnlyFile(ReadOnlyFile rof) {
                super(rof);
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
        } // BufferReadOnlyFile

        final class BufferInputStream extends DecoratingInputStream {
            boolean closed;

            BufferInputStream(InputStream in) {
                super(in);
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
        } // BufferInputStream

        @Immutable
        final class Nio2BufferOutputSocket extends BufferOutputSocket {
            @Override
            public SeekableByteChannel newSeekableByteChannel() throws IOException {
                return new BufferOutputChannel(getBoundSocket().newSeekableByteChannel());
            }
        } // Nio2BufferInputSocket

        @Immutable
        class BufferOutputSocket extends DecoratingOutputSocket<Entry> {
            BufferOutputSocket() {
                super(data.getOutputSocket());
            }

            @Override
            public final OutputStream newOutputStream() throws IOException {
                return new BufferOutputStream(getBoundSocket().newOutputStream());
            }
        } // BufferOutputSocket

        final class BufferOutputChannel extends DecoratingSeekableByteChannel {
            boolean closed;

            BufferOutputChannel(SeekableByteChannel sbc) {
                super(sbc);
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
        } // BufferOutputChannel

        final class BufferOutputStream extends DecoratingOutputStream {
            boolean closed;

            BufferOutputStream(OutputStream out) {
                super(out);
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
        } // BufferOutputStream
    } // Buffer
}
