/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.truezip.kernel.io;

import java.io.EOFException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;

/**
 * Provides utility methods for {@link SeekableByteChannel}s.
 * 
 * @author Christian Schlichtherle
 */
public final class SeekableByteChannels {

    /** Can't touch this - hammer time! */
    private SeekableByteChannels() { }

    /**
     * Reads a single byte from the given seekable byte channel.
     * 
     * @param sbc the seekable byte channel to read.
     * @return the read byte or -1 on end-of-file.
     * @throws IOException on any I/O failure.
     */
    public static int readByte(final SeekableByteChannel sbc)
    throws IOException {
        final ByteBuffer buf = ByteBuffer.allocate(1);
        return 1 != sbc.read(buf) ? -1 : buf.get(0) & 0xff;
    }

    /**
     * Reads until the given buffer is full or end-of-file has been reached.
     * 
     * @param  sbc the seekable byte channel to read.
     * @param  buffer the byte buffer to read fully.
     * @throws EOFException on end-of-file.
     * @throws IOException on any I/O failure.
     */
    public static void readFully(
            final SeekableByteChannel sbc,
            final ByteBuffer buffer)
    throws IOException {
        final int remaining = buffer.remaining();
        int n = 0;
        do {
            int read = sbc.read(buffer);
            if (0 > read)
                throw new EOFException();
            n += read;
        } while (n < remaining);
    }
}
