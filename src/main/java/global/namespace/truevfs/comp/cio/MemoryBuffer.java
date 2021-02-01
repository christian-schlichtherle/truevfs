/*
 * Copyright (C) 2005-2015 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package global.namespace.truevfs.comp.cio;

import global.namespace.truevfs.comp.io.ByteBufferChannel;
import global.namespace.truevfs.comp.io.DisconnectingSeekableChannel;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.util.EnumMap;
import java.util.Objects;
import java.util.Optional;

import static global.namespace.truevfs.comp.cio.Entry.Access.READ;
import static global.namespace.truevfs.comp.cio.Entry.Access.WRITE;

/**
 * An I/O buffer which shares its contents with a {@linkplain ByteBuffer byte buffer}.
 * <p>
 * If the reference to the {@linkplain #getBuffer backing buffer} is empty, then any attempt to start input from this
 * I/O buffer results in a {@link FileNotFoundException}.
 * The reference gets automatically set upon each call to {@code close()} on any {@link OutputStream} or
 * {@link SeekableByteChannel} which has been obtained from an {@link #output() output socket} for this I/O buffer.
 * The reference can also get explicitly set by calling the constructor {@link #MemoryBuffer(String, ByteBuffer)} or the
 * method {@link #setBuffer(Optional)}.
 *
 * @author Christian Schlichtherle
 */
@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
public class MemoryBuffer implements IoBuffer {

    private final String name;
    private int initialCapacity;
    private Optional<? extends ByteBuffer> buffer = Optional.empty();
    private final EnumMap<Access, Long> times = new EnumMap<>(Access.class);
    private int reads;
    private int writes;

    /**
     * Constructs a new memory buffer.
     * The reference to the {@linkplain #getBuffer() backing buffer} is set to empty.
     *
     * @param name            the name of this memory buffer.
     * @param initialCapacity the initial capacity of the next backing buffer
     *                        to allocate when starting output to this memory buffer.
     */
    public MemoryBuffer(String name, int initialCapacity) {
        this(name, Optional.empty(), initialCapacity);
    }

    /**
     * Constructs a new memory buffer.
     * The {@linkplain #getInitialCapacity() initial capacity} is set to the
     * capacity of the given backing buffer.
     *
     * @param name   the name of this memory buffer.
     * @param buffer the byte buffer with the contents to share with this
     *               memory buffer.
     */
    public MemoryBuffer(String name, ByteBuffer buffer) {
        this(name, Optional.of(buffer), buffer.capacity());
    }

    private MemoryBuffer(
            final String name,
            final Optional<? extends ByteBuffer> buffer,
            final int initialCapacity) {
        this.name = Objects.requireNonNull(name);
        setBuffer(buffer);
        setInitialCapacity(initialCapacity);
    }

    /**
     * Returns the initial capacity of the next backing buffer to allocate when
     * starting output to this memory buffer.
     *
     * @return The initial capacity of the next backing buffer to allocate when
     * starting output to this memory buffer.
     */
    public final int getInitialCapacity() {
        return this.initialCapacity;
    }

    /**
     * Sets the initial capacity of the next backing buffer to allocate when
     * starting output to this memory buffer.
     *
     * @param initialCapacity the initial capacity of the next backing buffer
     *                        to allocate when starting output to this memory buffer.
     */
    public final void setInitialCapacity(final int initialCapacity) {
        if (0 > initialCapacity) {
            throw new IllegalArgumentException("Negative initial capacity: " + initialCapacity);
        }
        this.initialCapacity = initialCapacity;
    }

    /**
     * Returns a {@linkplain ByteBuffer#duplicate() duplicate} of the optional backing buffer with the contents to
     * share with this memory buffer.
     * If the backing buffer is not set, then empty is returned.
     * Otherwise, the returned buffer's position is set to zero and its limit is set to the size of the contents of
     * this memory buffer.
     *
     * @return A {@linkplain ByteBuffer#duplicate() duplicate} of the optional backing buffer with the contents to share
     * with this memory buffer.
     */
    public final Optional<? extends ByteBuffer> getBuffer() {
        return buffer.map(ByteBuffer::duplicate);
    }

    /**
     * Sets the optional backing buffer with the contents to share with this memory buffer.
     * If {@code buffer} is not empty, a {@linkplain ByteBuffer#duplicate() duplicate} is made and
     * {@linkplain ByteBuffer#rewind() rewind} in order to protect this memory buffer from concurrent modifications of
     * the given buffer's properties.
     *
     * @param buffer the optional byte buffer with the contents to share with this memory buffer.
     */
    public final void setBuffer(Optional<? extends ByteBuffer> buffer) {
        this.buffer = buffer.map(ByteBuffer::duplicate).map(Buffer::rewind).map(ByteBuffer.class::cast);
    }

    @Override
    public final String getName() {
        return name;
    }

    @Override
    public final long getSize(Size type) {
        return buffer.map(Buffer::limit).orElse((int) UNKNOWN);
    }

    /**
     * @param type the access type.
     * @return The number of times an input or output connection to the backing
     * buffer has been opened.
     */
    // http://java.net/jira/browse/TRUEZIP-83
    public final int getCount(Access type) {
        return type == WRITE ? writes : reads;
    }

    /**
     * @return The last time an input or output connection to the backing
     * buffer has been {@code close()}d.
     */
    @Override
    public final long getTime(Access type) {
        final Long time = times.get(type);
        return null != time ? time : UNKNOWN;
    }

    @Override
    public Optional<Boolean> isPermitted(Access type, Entity entity) {
        return Optional.of(true);
    }

    @Override
    public final InputSocket<IoBuffer> input() {
        return new Input();
    }

    @Override
    public final OutputSocket<IoBuffer> output() {
        return new Output();
    }

    @Override
    public void release() throws IOException {
        buffer = Optional.empty();
    }

    /**
     * Returns a string representation of this object for debugging and logging
     * purposes.
     */
    @Override
    public String toString() {
        return String.format("%s[name=%s]",
                getClass().getName(),
                getName());
    }

    private SeekableByteChannel dataInputChannel() throws FileNotFoundException {
        return buffer
                .map(ByteBuffer::asReadOnlyBuffer)
                .map(ByteBufferChannel::new)
                .orElseThrow(FileNotFoundException::new);
    }

    private SeekableByteChannel dataOutputChannel() {
        final ByteBuffer buffer = (ByteBuffer) ByteBuffer
                .allocateDirect(initialCapacity)
                .limit(0);
        return new ByteBufferChannel(buffer);
    }

    private final class Input implements InputSocket<IoBuffer> {

        @Override
        public IoBuffer getTarget() {
            return MemoryBuffer.this;
        }

        @Override
        public SeekableByteChannel channel(Optional<? extends OutputSocket<? extends Entry>> peer) throws IOException {
            return new DataInputChannel();
        }
    }

    private final class Output implements OutputSocket<IoBuffer> {

        @Override
        public IoBuffer getTarget() {
            return MemoryBuffer.this;
        }

        @Override
        public SeekableByteChannel channel(Optional<? extends InputSocket<? extends Entry>> peer) throws IOException {
            return new DataOutputChannel();
        }
    }

    private final class DataInputChannel extends DisconnectingSeekableChannel {

        boolean closed;

        DataInputChannel() throws FileNotFoundException {
            super(dataInputChannel());
            reads++;
        }

        @Override
        public boolean isOpen() {
            return !closed;
        }

        @Override
        public void close() throws IOException {
            if (!closed) {
                channel.close();
                times.put(READ, System.currentTimeMillis());
                closed = true;
            }
        }
    }

    private final class DataOutputChannel extends DisconnectingSeekableChannel {

        boolean closed;

        DataOutputChannel() {
            super(dataOutputChannel());
            writes++;
        }

        @Override
        public boolean isOpen() {
            return !closed;
        }

        @Override
        public void close() throws IOException {
            if (!closed) {
                channel.close();
                times.put(WRITE, System.currentTimeMillis());
                buffer = Optional.of((ByteBuffer) ((ByteBufferChannel) channel).getBuffer().rewind());
                closed = true;
            }
        }
    }
}
