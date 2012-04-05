/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.truezip.kernel.io;

import edu.umd.cs.findbugs.annotations.CreatesObligation;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import javax.annotation.WillCloseWhenClosed;
import javax.annotation.WillNotClose;
import javax.annotation.concurrent.NotThreadSafe;

/**
 * Provides read-only access to an interval of its decorated seekable byte
 * channel.
 * Note that this class implements its own virtual file pointer.
 * 
 * @author Christian Schlichtherle
 */
@NotThreadSafe
public class IntervalReadOnlyChannel extends DecoratingReadOnlyChannel {

    /**
     * The start position of this channel in the decorated channel.
     */
    private final long start;

    /** The size of this channel. */
    private final long size;

    /**
     * Whether this channel may assume that it has exclusive access to the
     * decorated channel or not.
     * If this is {@code true}, then the position of the file pointer in the
     * decorated channel must be {@code start}!
     * If this is {@code false}, then the file pointer in the decorated channel
     * gets positioned before each read operation.
     */
    private final boolean exclusive;

    /**
     * The virtual file pointer for this channel.
     * This is relative to {@link #start}.
     */
    private long pos;

    /**
     * Constructs a new interval seekable byte channel starting at the current
     * position of the file pointer in the decorated seekable byte channel.
     * <p>
     * Note that this constructor assumes that it has exclusive access to the
     * decorated seekable byte channel.
     * Concurrent modification of the file pointer in the decorated seekable
     * byte channel may corrupt the input of this decorating seekable byte
     * channel!
     *
     * @param  sbc the seekable byte channel to decorate.
     * @param  size the size of the interval.
     * @throws IOException on any I/O failure.
     */
    @CreatesObligation
    @edu.umd.cs.findbugs.annotations.SuppressWarnings("OBL_UNSATISFIED_OBLIGATION")
    public IntervalReadOnlyChannel(
            final @WillCloseWhenClosed SeekableByteChannel sbc,
            final long size)
    throws IOException {
        this(sbc, sbc.position(), size, true);
    }

    /**
     * Constructs a new interval seekable byte channel starting at the given
     * position of the file pointer in the decorated seekable byte channel.
     * <p>
     * Note that this constructor assumes that it does <em>not</em> have
     * exclusive access to the decorated seekable byte channel and positions
     * the file pointer in the decorated seekable byte channel before each read
     * operation!
     *
     * @param  sbc the channel to decorate.
     * @param  start the start of the interval.
     * @param  size the size of the interval.
     * @throws IOException on any I/O failure.
     */
    @CreatesObligation
    @edu.umd.cs.findbugs.annotations.SuppressWarnings("OBL_UNSATISFIED_OBLIGATION")
    public IntervalReadOnlyChannel(
            final @WillNotClose SeekableByteChannel sbc,
            final long start,
            final long size)
    throws IOException {
        this(sbc, start, size, false);
    }
    
    private IntervalReadOnlyChannel(
            final SeekableByteChannel sbc,
            final long start,
            final long size,
            final boolean exclusive)
    throws IOException {
        super(sbc);
        if (start < 0 || size < 0 || sbc.size() < start + size)
            throw new IllegalArgumentException();
        this.start = start;
        this.size = size;
        this.exclusive = exclusive;
    }

    @Override
    public int read(final ByteBuffer dst) throws IOException {
        // Check no-op first for compatibility with FileChannel.
        int remaining = dst.remaining();
        if (0 >= remaining)
            return 0;

        // Check is open and not at EOF.
        final long size = size();
        final long pos = this.pos;
        if (pos >= size)
            return -1;

        // Setup.
        final long available = size - pos;
        final int limit;
        if (remaining > available) {
            remaining = (int) (available);
            limit = dst.limit();
            dst.limit(dst.position() + remaining);
        } else {
            limit = -1;
        }

        // Operate.
        final int read;
        try {
            if (!exclusive)
                sbc.position(start + pos);
            read = sbc.read(dst);
        } finally {
            if (0 <= limit)
                dst.limit(limit);
        }

        // Update state.
        this.pos = pos + read;

        // Post-check.
        if (0 == remaining) {
            // This was an attempt to read past the end of the file.
            // This could have been checked in advance, but its still desirable
            // to have the rof test its state - it might throw an
            // IOException if it has been closed before.
            assert 0 >= read;
            return -1;
        }
        assert 0 < read;
        return read;
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
        sbc.position(start + pos);
        this.pos = pos;
        return this;
    }

    @Override
    public long size() throws IOException {
        checkOpen();
        return size;
    }

    /**
     * Closes the decorated read only file if and only if it is exclusively
     * accessed by this decorating read only file.
     * 
     * @throws IOException On any I/O error.
     */
    @Override
    public void close() throws IOException {
        if (exclusive)
            sbc.close();
    }
}
