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

    private static final long INVALID = Long.MIN_VALUE;

    /** The virtual channel position. */
    private long pos;

    /**
     * The position in the decorated channel where the buffer starts.
     * This is always a multiple of the buffer size.
     */
    private long bufferStart = INVALID;

    /** The buffer for the channel data. */
    private final ByteBuffer buffer;

    /**
     * Constructs a new buffered read-only channel.
     *
     * @param channel the channel to decorate.
     */
    @CreatesObligation
    @edu.umd.cs.findbugs.annotations.SuppressWarnings("OBL_UNSATISFIED_OBLIGATION")
    public BufferedReadOnlyChannel(
            final @WillCloseWhenClosed SeekableByteChannel channel) {
        this(channel, Streams.BUFFER_SIZE);
    }

    /**
     * Constructs a new buffered read-only channel.
     *
     * @param channel the channel to decorate.
     * @param bufferSize the size of the byte buffer.
     */
    @CreatesObligation
    @edu.umd.cs.findbugs.annotations.SuppressWarnings("OBL_UNSATISFIED_OBLIGATION")
    public BufferedReadOnlyChannel(
            final @WillCloseWhenClosed SeekableByteChannel channel,
            final int bufferSize) {
        super(channel);
        buffer = ByteBuffer.allocate(bufferSize);
    }

    @Override
    public int read(final ByteBuffer dst) throws IOException {
        // Check no-op first for compatibility with FileChannel.
        final int remaining = dst.remaining();
        if (remaining <= 0)
            return 0;

        // Check is open and not at EOF.
        final long size = size();
        if (position() >= size) // ensure pos is initialized, but do NOT cache!
            return -1;

        // Read of buffer data.
        int total = 0; // amount of data copied dst
        while (total < remaining && pos < size) {
            positionBuffer();
            final int processed = copy(buffer, dst);
            assert processed > 0;
            total += processed;
            pos += processed;
        }

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

    /**
     * Notifies this channel of concurrent changes in its decorated channel.
     * Calling this method triggers a reload of the buffer on the next read
     * access.
     * 
     * @return {@code this}
     */
    public BufferedReadOnlyChannel sync() {
        bufferStart = INVALID;
        return this;
    }

    /**
     * Positions the buffer so that it holds the data
     * referenced by the virtual file pointer.
     *
     * @throws IOException on any I/O error.
     *         The buffer gets invalidated in this case.
     */
    private void positionBuffer() throws IOException {
        final ByteBuffer buffer = this.buffer;
        final int bufferSize = buffer.capacity();

        // Check position.
        final long pos = this.pos;
        long bufferStart = this.bufferStart;
        long bufferPos = pos - bufferStart;
        final long nextBufferStart = bufferStart + bufferSize;
        if (0 <= bufferPos && pos < nextBufferStart) {
            buffer.position((int) bufferPos);
            return;
        }

        try {
            final SeekableByteChannel channel = this.channel;

            // Move position.
            // Round down to multiple of buffer size
            this.bufferStart = bufferStart = pos / bufferSize * bufferSize;
            bufferPos = pos - bufferStart;
            if (bufferStart != nextBufferStart)
                channel.position(bufferStart);

            // Fill buffer until end of file or buffer.
            // This should normally complete in one loop cycle, but we do not
            // depend on this as it would be a violation of the contract for a
            // SeekableByteChannel.
            buffer.clear();
            int total = 0;
            do {
                final int read = channel.read(buffer);
                if (read < 0)
                    break;
                total += read;
            } while (total < bufferSize);
            buffer.flip().position((int) bufferPos);
        } catch (final Throwable ex) {
            this.bufferStart = INVALID;
            throw ex;
        }
    }
}
