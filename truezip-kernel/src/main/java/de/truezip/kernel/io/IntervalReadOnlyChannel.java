/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.truezip.kernel.io;

import edu.umd.cs.findbugs.annotations.CreatesObligation;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.NonWritableChannelException;
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
public class IntervalReadOnlyChannel extends DecoratingSeekableByteChannel {

    private final long start;
    private final long size;
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
     * @param  sbc the seekable byte channel to decorate.
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
    
    /**
     * Constructs a new interval seekable byte channel starting at the given
     * position of the file pointer in the given decorated seekable byte
     * channel.
     *
     * @param  sbc the seekable byte channel to decorate.
     * @param  start the start of the interval.
     * @param  size the size of the interval.
     * @param  exclusive whether this decorating sekable byte channel may
     *         assume it has exclusive access to the decorated seekable byte
     *         channel or not.
     *         If this is {@code true}, then the position of the file pointer
     *         in the decorated seekable byte channel must be {@code start}!
     *         If this is {@code false}, then the file pointer in the decorated
     *         seekable byte channel gets positioned before each read operation.
     * @throws IOException on any I/O failure.
     */
    @CreatesObligation
    @edu.umd.cs.findbugs.annotations.SuppressWarnings("OBL_UNSATISFIED_OBLIGATION")
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
        int remaining = dst.remaining();
        if (0 >= remaining) {
            // Be fault-tolerant and compatible to FileChannel, even if
            // the decorated read only file has been closed before.
            return 0;
        }

        // Setup.
        final long pos = this.pos;
        final long size = this.size;
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
    public int write(ByteBuffer src) throws IOException {
        throw new NonWritableChannelException();
    }

    @Override
    public long position() throws IOException {
        sbc.position(); // check state.
        return pos;
    }

    @Override
    public SeekableByteChannel position(final long pos) throws IOException {
        if (0 > pos)
            throw new IllegalArgumentException();
        final long size = this.size;
        if (pos > size)
            throw new IOException("Position (" + pos
                    + ") is larger than channel size (" + size + ")!");
        sbc.position(start + pos);
        this.pos = pos;
        return this;
    }

    @Override
    public long size() throws IOException {
        final long size = this.size;
        if (sbc.size() < start + size)
            throw new IOException("Seekable Byte Channel has been changed!");
        return size;
    }

    @Override
    public SeekableByteChannel truncate(long size) throws IOException {
        throw new NonWritableChannelException();
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
