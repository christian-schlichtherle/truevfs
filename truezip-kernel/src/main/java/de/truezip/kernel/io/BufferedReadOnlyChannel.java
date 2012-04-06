/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.truezip.kernel.io;

import static de.truezip.kernel.io.Buffers.copy;
import edu.umd.cs.findbugs.annotations.CreatesObligation;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import javax.annotation.WillCloseWhenClosed;
import javax.annotation.concurrent.NotThreadSafe;

/**
 * Provides buffered random read-only access to its decorated seekable byte
 * channel.
 * Note that this channel implements its own virtual position.
 *
 * @author Christian Schlichtherle
 */
@NotThreadSafe
public class BufferedReadOnlyChannel extends DecoratingReadOnlyChannel {

    /** The size of this channel. */
    private long size;

    /** The virtual position of this channel. */
    private long pos;

    /**
     * The position in the decorated channel where the buffer starts.
     * This is always a multiple of the buffer size.
     */
    private long bufferPos;

    /** The buffer to the data of the decorated channel. */
    private final ByteBuffer buffer;

    /**
     * Constructs a new buffered read-only channel.
     *
     * @param  channel the channel to decorate.
     * @throws IOException on any I/O failure.
     */
    @CreatesObligation
    @edu.umd.cs.findbugs.annotations.SuppressWarnings("OBL_UNSATISFIED_OBLIGATION")
    public BufferedReadOnlyChannel(
            final @WillCloseWhenClosed SeekableByteChannel channel)
    throws IOException {
        this(channel, Streams.BUFFER_SIZE);
    }

    /**
     * Constructs a new buffered input channel.
     *
     * @param  channel the channel to decorate.
     * @param  bufferSize the size of the byte buffer.
     * @throws IOException on any I/O failure.
     */
    @CreatesObligation
    @edu.umd.cs.findbugs.annotations.SuppressWarnings("OBL_UNSATISFIED_OBLIGATION")
    public BufferedReadOnlyChannel(
            final @WillCloseWhenClosed SeekableByteChannel channel,
            final int bufferSize)
    throws IOException {
        super(channel);
        buffer = ByteBuffer.allocate(bufferSize);
        assert bufferSize == buffer.limit();
        assert bufferSize == buffer.capacity();
        reset();
        pos = channel.position();
    }

    @Override
    public int read(final ByteBuffer dst) throws IOException {
        // Check no-op first for compatibility with FileChannel.
        final int remaining = dst.remaining();
        if (0 >= remaining)
            return 0;

        // Check is open and not at EOF.
        final long size = size();
        if (pos >= size) // do NOT cache pos!
            return -1;

        // Setup.
        final int bufferSize = buffer.limit();
        int total = 0; // amount of read data copied to dst

        {
            // Partial read of buffer data at the start.
            final int p = (int) (pos % bufferSize);
            if (p != 0) {
                // The virtual position is NOT starting on a buffer boundary.
                positionBuffer();
                buffer.position(p);
                total = copy(buffer, dst);
                assert total > 0;
                pos += total;
            }
        }

        {
            // Full read of buffer data in the middle.
            while (total + bufferSize < remaining && pos + bufferSize < size) {
                // The virtual position is starting on a buffer boundary.
                positionBuffer();
                buffer.rewind();
                final int copied = copy(buffer, dst);
                assert copied == bufferSize;
                total += copied;
                pos += copied;
            }
        }

        // Partial read of buffer data at the end.
        if (total < remaining && pos < size) {
            // The virtual position is starting on a buffer boundary.
            positionBuffer();
            buffer.rewind();
            final int copied = copy(buffer, dst);
            total += copied;
            pos += copied;
            assert copied > 0;
        }

        // Assert that at least one byte has been read.
        // Note that EOF has been checked before.
        assert 0 < total;
        return total;
    }

    @Override
    public long position() throws IOException {
        checkOpen();
        return pos;
    }

    @Override
    public SeekableByteChannel position(final long pos) throws IOException {
        if (0 > pos)
            throw new IllegalArgumentException();
        checkOpen();
        this.pos = pos;
        return this;
    }

    @Override
    public long size() throws IOException {
        checkOpen();
        final long size = this.size;
        return 0 <= size ? size : (this.size = channel.size());
    }

    /**
     * Notifies this channel of concurrent changes in its decorated channel.
     * Calling this method triggers a reload of the buffer on the next read
     * access.
     * 
     * @return {@code this}
     */
    public BufferedReadOnlyChannel sync() {
        reset();
        return this;
    }

    private void reset() {
        size = -1;
        invalidateBuffer();
    }

    /** Triggers a reload of the buffer on the next read access. */
    private void invalidateBuffer() {
        bufferPos = Long.MIN_VALUE;
    }

    /**
     * Positions the buffer so that it holds the data
     * referenced by the virtual file pointer.
     *
     * @throws IOException on any I/O failure.
     *         The buffer gets invalidated in this case.
     */
    private void positionBuffer() throws IOException {
        // Check position.
        final long pos = this.pos;
        final int bufferSize = buffer.limit();
        final long nextBufferPos = bufferPos + bufferSize;
        if (bufferPos <= pos && pos < nextBufferPos)
            return;

        try {
            // Move position.
            bufferPos = pos / bufferSize * bufferSize; // round down to multiple of buffer size
            if (bufferPos != nextBufferPos)
                channel.position(bufferPos);

            // Fill buffer until end of file or buffer.
            // This should normally complete in one loop cycle, but we do not
            // depend on this as it would be a violation of the contract for a
            // SeekableByteChannel.
            buffer.rewind();
            int n = 0;
            do {
                int read = channel.read(buffer);
                if (0 > read) {
                    size = bufferPos + n;
                    break;
                }
                n += read;
            } while (n < bufferSize);
        } catch (final Throwable ex) {
            invalidateBuffer();
            throw ex;
        }
    }
}
