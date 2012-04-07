/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.truezip.kernel.io;

import java.io.EOFException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.SeekableByteChannel;
import java.nio.channels.WritableByteChannel;

/**
 * Provides utility methods for {@link SeekableByteChannel}s.
 * 
 * @author Christian Schlichtherle
 */
final class Channels {

    /** Can't touch this - hammer time! */
    private Channels() { }

    /**
     * Reads a single byte from the given seekable byte channel.
     * 
     * @param channel the readable byte channel.
     * @return the read byte or -1 on end-of-file.
     * @throws IOException on any I/O failure.
     */
    public static int readByte(final ReadableByteChannel channel)
    throws IOException {
        final ByteBuffer buf = ByteBuffer.allocate(1);
        return 1 != channel.read(buf) ? -1 : buf.get(0) & 0xff;
    }

    /**
     * Marks the given buffer, reads all its remaining bytes from the given
     * channel and resets the buffer.
     * If an {@link IOException} occurs or the end-of-file is reached before
     * the buffer has been entirely filled, then it does not get reset and the
     * {@code IOException} or an {@link EOFException} gets thrown respectively.
     * 
     * @param  channel the channel.
     * @param  buffer the byte buffer to fill with data from the channel.
     * @throws EOFException on end-of-file.
     * @throws IOException on any I/O failure.
     */
    public static void readFully(
            final ReadableByteChannel channel,
            final ByteBuffer buffer)
    throws IOException {
        int remaining = buffer.remaining();
        buffer.mark();
        do {
            int read = channel.read(buffer);
            if (0 > read)
                throw new EOFException();
            remaining -= read;
        } while (0 < remaining);
        buffer.reset();
    }

    /**
     * Marks the given buffer, writes all its remaining bytes to the given
     * channel and resets the buffer.
     * If an {@link IOException} occurs, then the buffer does not get reset
     * and the {@code IOException} gets thrown.
     * 
     * @param  channel the channel.
     * @param  buffer the byte buffer with the data to flush to the channel.
     * @throws IOException on any I/O failure.
     */
    public static void writeFully(
            final WritableByteChannel channel,
            final ByteBuffer buffer)
    throws IOException {
        int remaining = buffer.remaining();
        buffer.mark();
        do {
            remaining -= channel.write(buffer);
        } while (0 < remaining);
        buffer.reset();
    }
}
