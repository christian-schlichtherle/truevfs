/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package de.schlichtherle.truezip.io;

import edu.umd.cs.findbugs.annotations.CleanupObligation;
import edu.umd.cs.findbugs.annotations.CreatesObligation;
import edu.umd.cs.findbugs.annotations.DischargesObligation;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ReadOnlyBufferException;
import java.nio.channels.SeekableByteChannel;
import javax.annotation.concurrent.NotThreadSafe;

/**
 * Adapts a {@linkplain ByteBuffer byte buffer} to a seekable byte channel.
 * 
 * @author  Christian Schlichtherle
 * @version $Id$
 */
@NotThreadSafe
@CleanupObligation
public class SeekableByteBufferChannel implements SeekableByteChannel {

    private ByteBuffer buffer;

    /**
     * Constructs a new seekable byte buffer channel with a
     * {@linkplain ByteBuffer#duplicate() duplicate} of the given byte buffer
     * as its initial {@linkplain #getByteBuffer() byte buffer}.
     * Note that the buffer contents are shared between the client application
     * and this class.
     * 
     * @param  buffer the initial byte buffer to read or write.
     * @throws IllegalArgumentException if {@code buffer} is not read-only and
     *         supports no {@linkplain ByteBuffer#array() array access} for
     *         resizing it.
     */
    @CreatesObligation
    public SeekableByteBufferChannel(final ByteBuffer buffer) {
        if (!buffer.isReadOnly() && !buffer.hasArray())
            throw new IllegalArgumentException();
        this.buffer = buffer.duplicate();
    }

    /**
     * Returns a {@linkplain ByteBuffer#duplicate() duplicate} of the backing
     * byte buffer.
     * Note that the buffer contents are shared between the client application
     * and this class.
     * 
     * @return A {@linkplain ByteBuffer#duplicate() duplicate} of the backing
     *         byte buffer.
     */
    public ByteBuffer getByteBuffer() {
        return buffer.duplicate();
    }

    @Override
    public final int read(final ByteBuffer dst) throws IOException {
        final int available = buffer.remaining();
        if (0 >= available)
            return -1;
        int remaining = dst.remaining();
        if (remaining > available)
            remaining = available;
        final int limit;
        if (available > remaining) {
            limit = buffer.limit();
            buffer.limit(buffer.position() + remaining);
        } else {
            limit = -1;
        }
        try {
            dst.put(buffer);
        } finally {
            if (0 <= limit)
                buffer.limit(limit);
        }
        return remaining;
    }

    @Override
    public final int write(final ByteBuffer src) throws IOException {
        final int remaining = src.remaining();
        final int position = buffer.position();
        ensureLimit(position + remaining);
        buffer.put(src);
        return remaining;
    }

    @Override
    public final long position() throws IOException {
        return buffer.position();
    }

    private void ensureLimit(final long minLimit) throws IOException {
        final int limit = buffer.limit();
        if (minLimit <= limit)
            return;
        if (buffer.isReadOnly())
            throw new ReadOnlyBufferException();
        final long oldCapacity = buffer.capacity();
        if (minLimit <= oldCapacity) {
            buffer.limit((int) minLimit);
        } else if (minLimit > Integer.MAX_VALUE) {
            throw new OutOfMemoryError();
        } else {
            long newCapacity = 0 < oldCapacity ? oldCapacity : 1;
            while ((newCapacity <<= 1) < minLimit) {
            }
            if (newCapacity > Integer.MAX_VALUE)
                newCapacity = minLimit;
            final byte[] array = new byte[(int) newCapacity];
            System.arraycopy(buffer.array(), buffer.arrayOffset(), array, 0, limit);
            buffer = ByteBuffer.wrap(array);
        }
    }

    @Override
    public final SeekableByteBufferChannel position(long newPosition)
    throws IOException {
        ensureLimit(newPosition);
        buffer.position((int) newPosition);
        return this;
    }

    @Override
    public final long size() throws IOException {
        return buffer.limit();
    }

    @Override
    public final SeekableByteBufferChannel truncate(final long newSize)
    throws IOException {
        if (buffer.limit() > newSize)
            buffer.limit((int) newSize);
        return this;
    }

    /**
     * Returns always {@code true}.
     * 
     * @return always {@code true}.
     */
    @Override
    public boolean isOpen() {
        return true;
    }

    /** A no-op. */
    @Override
    @DischargesObligation
    public void close() throws IOException {
    }
}
