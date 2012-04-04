/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.truezip.kernel.io;

import static de.truezip.kernel.io.ByteBuffers.copy;
import de.truezip.kernel.io.Streams;
import edu.umd.cs.findbugs.annotations.CreatesObligation;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.NonWritableChannelException;
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
public class BufferedReadOnlyChannel extends DecoratingSeekableByteChannel {

    /** The default size of the window byte buffer to the channel data. */
    public static final int WINDOW_LEN = Streams.BUFFER_SIZE;

    /** The size of this channel. */
    private long size;

    /**
     * The virtual file pointer for this channel.
     * This is relative to the start of this channel.
     */
    private long pos;

    /**
     * The position in this channel where the window byte buffer starts.
     * This is always a multiple of the window byte buffer size.
     */
    private long windowPos;

    /** The window byte buffer to the channel data. */
    private final ByteBuffer window;

    /**
     * Constructs a new buffered input channel.
     *
     * @param  sbc the seekable byte channel to decorate.
     * @throws IOException on any I/O failure.
     */
    @CreatesObligation
    @edu.umd.cs.findbugs.annotations.SuppressWarnings("OBL_UNSATISFIED_OBLIGATION")
    public BufferedReadOnlyChannel(
            final @WillCloseWhenClosed SeekableByteChannel sbc)
    throws IOException {
        this(sbc, WINDOW_LEN);
    }

    /**
     * Constructs a new buffered input channel.
     *
     * @param  sbc the seekable byte channel to decorate.
     * @param  windowLen the size of the buffer window in bytes.
     * @throws IOException on any I/O failure.
     */
    @CreatesObligation
    @edu.umd.cs.findbugs.annotations.SuppressWarnings("OBL_UNSATISFIED_OBLIGATION")
    public BufferedReadOnlyChannel(
            final @WillCloseWhenClosed SeekableByteChannel sbc,
            final int windowLen)
    throws IOException {
        super(sbc);
        this.pos = this.sbc.position();
        this.window = ByteBuffer.allocate(windowLen);
        invalidateWindow();
        assert 0 < this.window.capacity();
    }

    @Override
    public int read(final ByteBuffer dst)
    throws IOException {
        final int remaining = dst.remaining();
        if (0 >= remaining) {
            // Be fault-tolerant and compatible to FileChannel, even if
            // the decorated read only file has been closed before.
            return 0;
        }

        final long size = size(); // check state
        if (pos >= size)
            return -1;

        // Setup.
        final int capacity = window.capacity();
        int read = 0; // amount of read data copied to buf

        {
            // Partial read of window data at the start.
            final int p = (int) (pos % capacity);
            if (p != 0) {
                // The file pointer is not on a window boundary.
                positionWindow();
                window.position(p);
                pos += read = copy(window, dst);
            }
        }

        {
            // Full read of window data in the middle.
            while (read + capacity < remaining && pos + capacity <= size) {
                // The file pointer is starting and ending on window boundaries.
                positionWindow();
                window.position(0);
                copy(window, dst);
                read += capacity;
                pos += capacity;
            }
        }

        // Partial read of window data at the end.
        if (read < remaining && pos < size) {
            // The file pointer is not on a window boundary.
            positionWindow();
            window.position(0);
            final int n = copy(window, dst);
            read += n;
            pos += n;
        }

        // Assert that at least one byte has been read if len isn't zero.
        // Note that EOF has been tested before.
        assert 0 < read;
        return read;
    }

    @Override
    public int write(ByteBuffer src) throws IOException {
        throw new NonWritableChannelException();
    }

    @Override
    public long position() throws IOException {
        sbc.position(); // check state
        return pos;
    }

    @Override
    public SeekableByteChannel position(final long pos) throws IOException {
        if (0 > pos)
            throw new IllegalArgumentException();
        final long size = size();
        if (pos > size)
            throw new IOException("Position (" + pos
                    + ") is larger than channel size (" + size + ")!");
        this.pos = pos;
        return this;
    }

    @Override
    public long size() throws IOException {
        final long size = sbc.size();
        if (size != this.size) {
            this.size = size;
            invalidateWindow();
        }
        return size;
    }

    @Override
    public SeekableByteChannel truncate(long size) throws IOException {
        throw new NonWritableChannelException();
    }

    //
    // Window byte buffer operations.
    //

    /**
     * Forces a reload of the window byte buffer on the next call to
     * {@link #positionWindow()}.
     */
    private void invalidateWindow() {
        windowPos = Long.MIN_VALUE;
    }

    /**
     * Positions the window byte buffer so that the block referenced by the
     * virtual file pointer is loaded.
     *
     * @throws IOException on any I/O failure.
     *         The window byte buffer gets invalidated in this case.
     */
    private void positionWindow()
    throws IOException {
        // Check position of window byte buffer.
        final long pos = this.pos;
        final int capacity = window.capacity();
        final long nextWindowOff = windowPos + capacity;
        if (windowPos <= pos && pos < nextWindowOff)
            return;
        // The window byte buffer needs to move.
        try {
            // Move window byte buffer.
            windowPos = (pos / capacity) * capacity; // round down to multiple of window capacity
            if (windowPos != nextWindowOff)
                sbc.position(windowPos);
            // Fill window byte buffer until end of file or buffer.
            // This should normally complete in one loop cycle, but we do not
            // depend on this as it would be a violation of the contract for a
            // SeekableByteBuffer.
            window.rewind();
            int n = 0;
            do {
                int read = sbc.read(window);
                if (0 > read)
                    break;
                n += read;
            } while (n < capacity);
        } catch (final IOException ex) {
            windowPos = -capacity - 1; // force position() at next positionWindow()
            throw ex;
        }
    }
}
