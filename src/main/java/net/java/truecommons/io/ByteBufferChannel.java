/*
 * Copyright (C) 2005-2015 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truecommons.io;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ReadOnlyBufferException;
import java.nio.channels.NonWritableChannelException;

/**
 * Adapts a {@linkplain ByteBuffer byte buffer} to a seekable byte channel.
 * The contents of the backing buffer are shared with this channel and any
 * changes will be visible to both parties.
 * However, the properties of the backing buffer are not shared with this
 * channel, e.g. changing the position of the backing buffer will not affect
 * the position of this channel and vice versa.
 * <p>
 * The initial position of the channel is always zero, independent of the
 * backing buffer's position.
 * When reading, the channel's position gets advanced until it hits the
 * backing buffer's limit.
 * When writing, the channel's position gets advanced and the backing
 * buffer's limit gets extended as required until it hits the backing
 * buffer's capacity.
 * When writing past the backing buffer's capacity, a new backing buffer is
 * allocated with a larger capacity and filled with the contents of the
 * old backing buffer.
 * Therefore, in order to avoid excessive buffer copy operations, clients
 * should configure this channel with a byte buffer with a capacity which is
 * large enough to host any data to write.
 * Furthermore, when closing this channel, clients should call
 * {@link #getBuffer()} to obtain a duplicate of the current backing buffer.
 *
 * @author Christian Schlichtherle
 */
public final class ByteBufferChannel extends AbstractSeekableChannel {

    /** The backing buffer with the contents to share. */
    private ByteBuffer buffer;

    /**
     * The position of this channel.
     * Note that {@code buffer.position() can't get used.
     *
     * @see SeekableByteChannel#position(long)
     */
    private long position;

    private boolean closed;

    /**
     * Constructs a new byte buffer channel which shares its contents with the
     * given byte {@code buffer}.
     * A {@linkplain ByteBuffer#duplicate() duplicate} of {@code buffer} is
     * made and {@linkplain ByteBuffer#rewind() rewind} in order to protect
     * this channel from concurrent modifications of the given buffer's
     * properties.
     * <p>
     * Since TrueCommons 2.1, this constructor accepts writable direct byte
     * buffers, too.
     *
     * @param buffer the byte buffer with the contents to share with this
     *        channel.
     */
    public ByteBufferChannel(final ByteBuffer buffer) {
        this.buffer = (ByteBuffer) buffer.duplicate().rewind();
    }

    /**
     * Returns a {@linkplain ByteBuffer#duplicate() duplicate} of the backing
     * buffer.
     * The returned buffer's position reflects only the last successful
     * {@code read} or {@code write} operation.
     * However, mere changes to the position of this channel are not reflected.
     * <p>
     * The returned buffer will be direct if and only if the initial backing
     * buffer is direct.
     * Likewise, the returned buffer will be read-only if and only if the
     * initial backing buffer is read-only.
     *
     * @return A {@linkplain ByteBuffer#duplicate() duplicate} of the backing
     *         buffer.
     */
    public ByteBuffer getBuffer() { return buffer.duplicate(); }

    @Override
    public int read(final ByteBuffer dst) throws IOException {
        checkOpen();
        int remaining = dst.remaining();
        if (remaining <= 0)
            return 0;
        final long oldPosition = this.position;
        final ByteBuffer buffer = this.buffer;
        if (oldPosition >= buffer.limit())
            return -1;
        buffer.position((int) oldPosition);
        final int available = buffer.remaining();
        final int srcLimit;
        if (available > remaining) {
            srcLimit = buffer.limit();
            buffer.limit(buffer.position() + remaining);
        } else {
            srcLimit = -1;
            remaining = available;
        }
        try {
            dst.put(buffer);
        } finally {
            if (0 <= srcLimit) buffer.limit(srcLimit);
        }
        assert buffer.position() == oldPosition + remaining;
        this.position += remaining;
        return remaining;
    }

    @Override
    public int write(final ByteBuffer src) throws IOException {
        checkOpen();
        if (this.position > Integer.MAX_VALUE)
            throw new OutOfMemoryError();
        final int oldPosition = (int) this.position;
        final int remaining = src.remaining();
        final int newPosition = oldPosition + remaining; // may overflow!
        ByteBuffer buffer = this.buffer;
        final int oldLimit = buffer.limit();
        if (0 > oldLimit - newPosition) { // mind overflow!
            final int oldCapacity = buffer.capacity();
            if (0 <= oldCapacity - newPosition) { // mind overflow!
                assert 0 <= newPosition;
                buffer.limit(newPosition).position(oldPosition);
            } else if (0 > newPosition) {
                throw new OutOfMemoryError();
            } else {
                if (buffer.isReadOnly())
                    throw new NonWritableChannelException();
                int newCapacity = oldCapacity << 1;
                if (0 > newCapacity - newPosition)
                    newCapacity = newPosition;
                if (0 > newCapacity)
                    newCapacity = Integer.MAX_VALUE;
                assert newPosition <= newCapacity;
                this.buffer = buffer = (ByteBuffer) (buffer.isDirect()
                        ? ByteBuffer.allocateDirect(newCapacity)
                        : ByteBuffer.allocate(newCapacity))
                        .put((ByteBuffer) buffer.position(0).limit(oldPosition))
                        .limit(newPosition);
            }
        } else {
            buffer.position(oldPosition);
        }
        assert buffer.position() == oldPosition;
        try {
            buffer.put(src);
        } catch (final ReadOnlyBufferException ex) {
            throw new NonWritableChannelException();
        }
        assert buffer.position() == newPosition;
        this.position = newPosition;
        return remaining;
    }

    @Override
    public long position() throws IOException {
        checkOpen();
        return position;
    }

    @Override
    public ByteBufferChannel position(long newPosition) throws IOException {
        checkOpen();
        if (0 > newPosition)
            throw new IllegalArgumentException();
        this.position = newPosition;
        return this;
    }

    @Override
    public long size() throws IOException {
        checkOpen();
        return buffer.limit();
    }

    @Override
    public ByteBufferChannel truncate(final long size) throws IOException {
        checkOpen();
        if (buffer.isReadOnly())
            throw new NonWritableChannelException();
        if (buffer.limit() > size)
            buffer.limit((int) size);
        if (position > size)
            position = size;
        return this;
    }

    @Override
    public boolean isOpen() { return !closed; }

    @Override
    public void close() { closed = true; }
}
