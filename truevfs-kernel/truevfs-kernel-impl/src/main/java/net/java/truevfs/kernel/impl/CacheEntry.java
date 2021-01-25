/*
 * Copyright © 2005 - 2021 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.kernel.impl;

import edu.umd.cs.findbugs.annotations.CleanupObligation;
import lombok.val;
import net.java.truecommons.cio.*;
import net.java.truecommons.io.DecoratingInputStream;
import net.java.truecommons.io.DecoratingOutputStream;
import net.java.truecommons.io.DecoratingSeekableChannel;
import net.java.truecommons.io.ReadOnlyChannel;
import net.java.truecommons.shed.Pool;
import net.java.truecommons.shed.Releasable;

import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import java.io.*;
import java.nio.channels.SeekableByteChannel;
import java.util.Optional;
import java.util.function.Function;

/**
 * Provides caching services for input and output sockets with the following features:
 * <ul>
 * <li>Upon the first read operation, the entry data will be read from the backing store and stored in the cache.
 *     Subsequent or concurrent read operations will be served from the cache without re-reading the entry data from the
 *     backing store again until the cache gets `release`d.
 * <li>At the discretion of the {@link CacheEntry.Strategy}, entry data written to the cache may not be written to the
 *     backing store until the cache gets `flush`ed.
 * <li>After a write operation, the entry data will be stored in the cache for subsequent read operations until the
 *     cache gets `release`d.
 * <li>As a side effect, caching decouples the backing store from its clients, allowing it to create, read, update or
 *     delete the entry data while some clients are still busy on reading or writing the cached entry data.
 * </ul>
 * Note that you need to call `configure` before you can do any input or output.
 *
 * @author Christian Schlichtherle
 */
@NotThreadSafe
@CleanupObligation
final class CacheEntry implements Entry, Releasable<IOException>, Flushable, Closeable {

    private final IoBufferPool pool;

    private final InputBufferPool inputBufferPool = new InputBufferPool();
    private final OutputBufferPool outputBufferPool;

    private Optional<Buffer> _buffer = Optional.empty();

    private Optional<InputSocket<? extends Entry>> input = Optional.empty();
    private Optional<OutputSocket<? extends Entry>> output = Optional.empty();

    /**
     * @param strategy the caching strategy.
     * @param pool     the pool for allocating and releasing temporary I/O entries.
     */
    private CacheEntry(final Function<CacheEntry, OutputBufferPool> strategy, final IoBufferPool pool) {
        this.pool = pool;
        outputBufferPool = strategy.apply(this);
    }

    private Optional<Buffer> buffer() {
        return _buffer;
    }

    private void buffer(final Optional<Buffer> nb) throws IOException {
        final Optional<Buffer> ob = _buffer;
        if (!ob.equals(nb)) {
            _buffer = nb;
            if (ob.isPresent()) {
                final Buffer b = ob.get();
                if (0 == b.writers && 0 == b.readers) {
                    b.release();
                }
            }
        }
    }

    @Override
    public String getName() {
        return "Johnny Cache!";
    }

    @Override
    public long getSize(final Size type) {
        return buffer().map(b -> b.getSize(type)).orElse((long) UNKNOWN);
    }

    @Override
    public long getTime(Access type) {
        return buffer().map(b -> b.getTime(type)).orElse((long) UNKNOWN);
    }

    /**
     * Returns `null` in order to block copying of access permissions of cache entries.
     */
    @Nullable
    @Override
    public Boolean isPermitted(Access type, Entity entity) {
        return null;
    }

    /**
     * Configures the input socket for reading the entry data from the backing store.
     * This method needs to be called before any input can be done - otherwise a {@link NullPointerException} will be
     * thrown on the first read attempt.
     * Note that calling this method does ''not'' `release` this cache.
     *
     * @param input an input socket for reading the entry data from the backing store.
     * @return `this`
     */
    CacheEntry configure(final InputSocket<? extends Entry> input) {
        this.input = Optional.of(input);
        return this;
    }

    /**
     * Configures the output socket for writing the entry data to the backing store.
     * This method needs to be called before any output can be done - otherwise a {@link NullPointerException} will be
     * thrown on the first write attempt.
     * Note that calling this method does ''not'' `flush` this cache.
     *
     * @param output an output socket for writing the entry data to the
     *               backing store.
     * @return `this`
     */
    CacheEntry configure(final OutputSocket<? extends Entry> output) {
        this.output = Optional.of(output);
        return this;
    }

    /**
     * Returns an input socket for reading the cached entry data.
     */
    InputSocket<? extends Entry> input() {
        class Foo extends DelegatingInputSocket<Entry> implements BufferAllocator {

            Buffer allocated;

            @Override
            public Buffer getAllocated() {
                return allocated;
            }

            @Override
            public void setAllocated(final Buffer allocated) {
                this.allocated = allocated;
            }

            @Override
            public InputSocket<? extends Buffer> socket() throws IOException {
                return buffer(inputBufferPool).input();
            }

            @Override
            public Entry target() throws IOException {
                return target(input.get());
            }
        }
        return new Foo();
    }

    /**
     * Returns an output socket for writing the cached entry data.
     */
    OutputSocket<? extends Entry> output() {
        class Bar extends DelegatingOutputSocket<Entry> implements BufferAllocator {

            Buffer allocated;

            @Override
            public Buffer getAllocated() {
                return allocated;
            }

            @Override
            public void setAllocated(final Buffer allocated) {
                this.allocated = allocated;
            }

            @Override
            public OutputSocket<? extends Entry> socket() throws IOException {
                return buffer(outputBufferPool).output();
            }

            @Override
            public Entry target() throws IOException {
                return target(output.get());
            }
        }
        return new Bar();
    }

    /**
     * Writes the cached entry data to the backing store unless already done.
     * Whether or not this method needs to be called depends on the caching strategy.
     * E.g. the caching strategy {@link Strategy#WriteThrough} writes any changed entry data immediately, so calling
     * this method has no effect.
     */
    @Override
    public void flush() throws IOException {
        if (buffer().isPresent()) {
            outputBufferPool.release(buffer().get());
        }
    }

    /**
     * Clears the entry data from this cache without flushing it.
     *
     * @throws IOException on any I/O error.
     */
    @Override
    public void release() throws IOException {
        buffer(Optional.empty());
    }

    @Override
    public void close() throws IOException {
        flush();
        release();
    }

    private interface BufferAllocator {

        Buffer getAllocated();

        void setAllocated(Buffer allocated);

        default Buffer buffer(Pool<Buffer, IOException> pool) throws IOException {
            val buffer = pool.allocate();
            setAllocated(buffer);
            return buffer;
        }

        default Entry target(IoSocket<? extends Entry> socket) throws IOException {
            val buffer = getAllocated();
            if (null != buffer) {
                return buffer;
            } else {
                return new ProxyEntry(socket.target());
            }
        }
    }

    /**
     * Defines different cache entry strategies.
     */
    enum Strategy implements Function<CacheEntry, OutputBufferPool> {

        /**
         * The write-through strategy flushes any written data as soon as the output stream created by
         * {@link CacheEntry#output} gets {@code close()}d.
         */
        WriteThrough() {

            @Override
            public OutputBufferPool apply(CacheEntry cache) {
                return cache.new WriteThroughOutputBufferPool();
            }
        },

        /**
         * The write-back strategy flushes any written data if and only if it gets explicitly
         * {@link CacheEntry#flush()}ed.
         */
        WriteBack() {

            @Override
            public OutputBufferPool apply(CacheEntry cache) {
                return cache.new WriteBackOutputBufferPool();
            }
        };

        final CacheEntry newCacheEntry(IoBufferPool pool) {
            return new CacheEntry(this, pool);
        }
    }

    private final class InputBufferPool implements Pool<Buffer, IOException> {

        @Override
        public Buffer allocate() throws IOException {
            final Buffer b;
            if (buffer().isPresent()) {
                b = buffer().get();
            } else {
                b = new Buffer();
                try {
                    b.load(input.get());
                } catch (Throwable t1) {
                    try {
                        b.release();
                    } catch (Throwable t2) {
                        t1.addSuppressed(t2);
                    }
                    throw t1;
                }
                buffer(Optional.of(b));
            }
            b.readers += 1;
            return b;
        }

        @Override
        public void release(final Buffer b) throws IOException {
            if (0 < b.readers) {
                b.readers -= 1;
                if (0 == b.readers && 0 == b.writers && b != buffer().orElse(null)) {
                    b.release();
                }
            }
        }
    }

    private final class WriteThroughOutputBufferPool extends OutputBufferPool {

        @Override
        public void release(final Buffer b) throws IOException {
            if (0 != b.writers) {
                super.release(b);
            }
        }
    }

    private final class WriteBackOutputBufferPool extends OutputBufferPool {

        @Override
        public void release(final Buffer b) throws IOException {
            if (0 != b.writers) {
                if (buffer().orElse(null) != b) {
                    buffer(Optional.of(b));
                } else {
                    super.release(b);
                }
            }
        }
    }

    private abstract class OutputBufferPool implements Pool<Buffer, IOException> {

        @Override
        public Buffer allocate() throws IOException {
            final Buffer b = new Buffer();
            assert 0 == b.readers;
            b.writers = 1;
            return b;
        }

        @Override
        public void release(final Buffer b) throws IOException {
            b.writers = 0;
            try {
                b.save(output.get());
            } finally {
                buffer(Optional.of(b));
            }
        }
    }

    /**
     * An I/O buffer for the cached contents.
     */
    private final class Buffer implements IoBuffer {

        private final IoBuffer data = pool.allocate();

        private int readers, writers;

        private Buffer() throws IOException {
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

        @Nullable
        @Override
        public Boolean isPermitted(Access type, Entity entity) {
            return data.isPermitted(type, entity);
        }

        void load(InputSocket<? extends Entry> input) throws IOException {
            IoSockets.copy(input, data.output());
        }

        void save(OutputSocket<? extends Entry> output) throws IOException {
            IoSockets.copy(data.input(), output);
        }

        @Override
        public void release() throws IOException {
            assert 0 == writers;
            assert 0 == readers;
            data.release();
        }

        @Override
        public InputSocket<Buffer> input() {
            return new AbstractInputSocket<Buffer>() {

                final InputSocket<? extends Entry> socket = data.input();

                @Override
                public Buffer target() {
                    return Buffer.this;
                }

                @Override
                public InputStream stream(OutputSocket<? extends Entry> peer) throws IOException {
                    return new CacheInputStream(socket.stream(peer));
                }

                @Override
                public SeekableByteChannel channel(OutputSocket<? extends Entry> peer) throws IOException {
                    return new CacheReadOnlyChannel(socket.channel(peer));
                }
            };
        }

        @Override
        public OutputSocket<Buffer> output() {
            return new AbstractOutputSocket<Buffer>() {

                final OutputSocket<? extends Entry> socket = data.output();

                @Override
                public Buffer target() {
                    return Buffer.this;
                }

                @Override
                public OutputStream stream(InputSocket<? extends Entry> peer) throws IOException {
                    return new CacheOutputStream(socket.stream(peer));
                }

                @Override
                public SeekableByteChannel channel(InputSocket<? extends Entry> peer) throws IOException {
                    return new CacheSeekableChannel(socket.channel(peer));
                }
            };
        }

        final class CacheInputStream extends DecoratingInputStream {

            CacheInputStream(InputStream in) {
                super(in);
            }

            private boolean closed;

            @Override
            public void close() throws IOException {
                // HC SUNT DRACONES!
                if (!closed) {
                    super.close();
                    inputBufferPool.release(Buffer.this);
                    closed = true; // TODO: Move up!
                }
            }
        }

        final class CacheReadOnlyChannel extends ReadOnlyChannel {

            CacheReadOnlyChannel(SeekableByteChannel channel) {
                super(channel);
            }

            private boolean closed;

            @Override
            public final void close() throws IOException {
                // HC SUNT DRACONES!
                if (!closed) {
                    super.close();
                    inputBufferPool.release(Buffer.this);
                    closed = true; // TODO: Move up!
                }
            }
        }

        final class CacheOutputStream extends DecoratingOutputStream {

            CacheOutputStream(OutputStream out) {
                super(out);
            }

            private boolean closed;

            @Override
            public final void close() throws IOException {
                // HC SUNT DRACONES!
                if (!closed) {
                    super.close();
                    outputBufferPool.release(Buffer.this);
                    closed = true; // TODO: Move up!
                }
            }
        }

        final class CacheSeekableChannel extends DecoratingSeekableChannel {

            CacheSeekableChannel(SeekableByteChannel channel) {
                super(channel);
            }

            private boolean closed;

            @Override
            public final void close() throws IOException {
                // HC SUNT DRACONES!
                if (!closed) {
                    super.close();
                    outputBufferPool.release(Buffer.this);
                    closed = true; // TODO: Move up!
                }
            }
        }
    }

    /**
     * Used to proxy the backing store entries.
     */
    private static final class ProxyEntry extends DecoratingEntry<Entry> {

        ProxyEntry(Entry entry) {
            super(entry);
        }
    }
}
