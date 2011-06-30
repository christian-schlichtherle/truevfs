/*
 * Copyright 2011 Schlichtherle IT Services
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.schlichtherle.truezip.io;

import edu.umd.cs.findbugs.annotations.DefaultAnnotation;
import edu.umd.cs.findbugs.annotations.NonNull;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ReadOnlyBufferException;
import java.nio.channels.SeekableByteChannel;
import net.jcip.annotations.NotThreadSafe;

/**
 * Adapts a byte buffer to a seekable byte channel.
 * 
 * @author  Christian Schlichtherle
 * @version $Id$
 */
@NotThreadSafe
@DefaultAnnotation(NonNull.class)
public class SeekableByteBufferChannel implements SeekableByteChannel {

    private ByteBuffer buffer;

    /**
     * Constructs a new seekable byte buffer channel with a
     * {@link ByteBuffer#duplicate() duplicate} of the given byte buffer as
     * its initial byte buffer.
     * Note that the buffer contents are shared between the client application
     * and this class.
     * 
     * @param  buffer the initial byte buffer to read or write.
     * @throws IllegalArgumentException if {@code buffer} supports no
     *         {@link ByteBuffer#array() array} access for copying data using
     *         relative bulk {@link ByteBuffer#get(byte[], int, int) get}
     *         and {@link ByteBuffer#put(byte[], int, int) put} methods.
     */
    public SeekableByteBufferChannel(final ByteBuffer buffer) {
        if (!buffer.hasArray())
            throw new IllegalArgumentException();
        this.buffer = buffer.duplicate();
    }

    /**
     * Returns a {@link ByteBuffer#duplicate() duplicate} of the backing byte
     * buffer.
     * Note that the buffer contents are shared between the client application
     * and this class.
     * 
     * @return A {@link ByteBuffer#duplicate() duplicate} of the backing byte
     *         buffer.
     */
    public ByteBuffer getByteBuffer() {
        return buffer.duplicate();
    }

    @Override
    public final int read(final ByteBuffer dst) {
        final int available = buffer.remaining();
        if (available <= 0)
            return -1;
        int remaining = dst.remaining();
        if (remaining > available)
            remaining = available;
        final int position = buffer.position();
        dst.put(buffer.array(), buffer.arrayOffset() + position, remaining);
        buffer.position(position + remaining);
        return remaining;
    }

    @Override
    public final int write(final ByteBuffer src) {
        final int remaining = src.remaining();
        final int position = buffer.position();
        ensureLimit(position + remaining);
        src.get(buffer.array(), buffer.arrayOffset() + position, remaining);
        buffer.position(position + remaining);
        return remaining;
    }

    @Override
    public final long position() {
        return buffer.position();
    }

    private void ensureLimit(final long minLimit) {
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
            long newCapacity = oldCapacity << 1;
            if (newCapacity > Integer.MAX_VALUE)
                newCapacity = minLimit;
            final byte[] array = new byte[(int) newCapacity];
            System.arraycopy(buffer.array(), buffer.arrayOffset(), array, 0, limit);
            buffer = ByteBuffer.wrap(array);
        }
    }

    @Override
    public final SeekableByteBufferChannel position(long newPosition) {
        ensureLimit(newPosition);
        buffer.position((int) newPosition);
        return this;
    }

    @Override
    public final long size() {
        return buffer.limit();
    }

    @Override
    public final SeekableByteBufferChannel truncate(final long newSize) {
        if (newSize < buffer.limit())
            buffer.limit((int) newSize);
        return this;
    }

    /**
     * Returns {@code true}.
     * 
     * @return {@code true}.
     */
    @Override
    public boolean isOpen() {
        return true;
    }

    /** A no-op. */
    @Override
    public void close() throws IOException {
    }
}
