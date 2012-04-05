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
 * Note that this class implements its own virtual file pointer.
 *
 * @author Christian Schlichtherle
 */
@NotThreadSafe
public class BufferedReadOnlyChannel extends DecoratingReadOnlyChannel {

    /** The default capacity of the byte buffer for the channel data. */
    private static final int BUFFER_CAPACITY = Streams.BUFFER_SIZE;

    /** The size of this channel. */
    private long size;

    /**
     * The virtual file pointer for this channel.
     * This is relative to the start of this channel.
     */
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
     * @param  sbc the channel to decorate.
     * @throws IOException on any I/O failure.
     */
    @CreatesObligation
    @edu.umd.cs.findbugs.annotations.SuppressWarnings("OBL_UNSATISFIED_OBLIGATION")
    public BufferedReadOnlyChannel(
            final @WillCloseWhenClosed SeekableByteChannel sbc)
    throws IOException {
        this(sbc, BUFFER_CAPACITY);
    }

    /**
     * Constructs a new buffered input channel.
     *
     * @param  sbc the seekable byte channel to decorate.
     * @param  capacity the size of the byte buffer.
     * @throws IOException on any I/O failure.
     */
    @CreatesObligation
    @edu.umd.cs.findbugs.annotations.SuppressWarnings("OBL_UNSATISFIED_OBLIGATION")
    public BufferedReadOnlyChannel(
            final @WillCloseWhenClosed SeekableByteChannel sbc,
            final int capacity)
    throws IOException {
        super(sbc);
        buffer = ByteBuffer.allocate(capacity);
        assert capacity == buffer.capacity();
        assert capacity == buffer.limit();
        reset();
        pos = sbc.position();
    }

    @Override
    public int read(final ByteBuffer dst) throws IOException {
        // Check no-op first for compatibility with FileChannel.
        int remaining = dst.remaining();
        if (0 >= remaining)
            return 0;

        // Check is open and not at EOF.
        final long size = size();
        if (pos >= size) // do NOT cache pos!
            return -1;

        // Setup.
        final int limit = buffer.limit();
        int copied, total = 0; // amount of read data copied to buf

        {
            // Partial read of buffer data at the start.
            final int p = (int) (pos % limit);
            if (p != 0) {
                // The file pointer is not on a buffer boundary.
                positionBuffer();
                buffer.position(p);
                pos += total = copied = copy(buffer, dst);
                assert copied > 0;
            }
        }

        {
            // Full read of buffer data in the middle.
            while (total + limit < remaining && pos + limit <= size) {
                // The file pointer is starting and ending on buffer boundaries.
                positionBuffer();
                buffer.rewind();
                copied = copy(buffer, dst);
                total += copied;
                pos += copied;
                assert copied == limit;
            }
        }

        // Partial read of buffer data at the end.
        if (total < remaining && pos < size) {
            // The file pointer is not on a buffer boundary.
            positionBuffer();
            buffer.rewind();
            copied = copy(buffer, dst);
            total += copied;
            pos += copied;
            assert copied > 0;
        }

        // Assert that at least one byte has been read if len isn't zero.
        // Note that EOF has been tested before.
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
        return 0 <= size ? size : (this.size = sbc.size());
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
     * Positions the buffer so that it holds the data referenced by the virtual
     * file pointer.
     *
     * @throws IOException on any I/O failure.
     *         The buffer gets invalidated in this case.
     */
    private void positionBuffer()
    throws IOException {
        // Check position of buffer.
        final long pos = this.pos;
        final int limit = buffer.limit();
        final long nextWindowPos = bufferPos + limit;
        if (bufferPos <= pos && pos < nextWindowPos)
            return;

        // The buffer needs to move.
        try {
            // Move buffer.
            bufferPos = (pos / limit) * limit; // round down to multiple of buffer limit
            if (bufferPos != nextWindowPos)
                sbc.position(bufferPos);

            // Fill buffer until end of file or buffer.
            // This should normally complete in one loop cycle, but we do not
            // depend on this as it would be a violation of the contract for a
            // SeekableByteChannel.
            buffer.rewind();
            int n = 0;
            do {
                int read = sbc.read(buffer);
                if (0 > read) {
                    size = bufferPos + n;
                    break;
                }
                n += read;
            } while (n < limit);
        } catch (final IOException ex) {
            invalidateBuffer();
            throw ex;
        }
    }
}
