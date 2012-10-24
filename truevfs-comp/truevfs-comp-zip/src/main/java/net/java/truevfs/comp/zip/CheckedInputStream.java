/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.comp.zip;

import java.io.IOException;
import java.io.InputStream;
import java.util.zip.Checksum;

/**
 * An input stream that also maintains a checksum of the data being read.
 * The checksum can then be used to verify the integrity of the input data.
 * <p>
 * In constrast to its super class, this class accepts a parameter to customize
 * the skip buffer size.
 *
 * @author Christian Schlichtherle
 */
class CheckedInputStream extends java.util.zip.CheckedInputStream {
    private final int skipBufferSize;

    /**
     * Constructs an input stream using the specified checksum and skip buffer
     * size.
     *
     * @param in the input stream
     * @param cksum the checksum
     * @param skipBufferSize the skip buffer size
     */
    CheckedInputStream(
            final InputStream in,
            final Checksum cksum,
            final int skipBufferSize) {
        super(in, cksum);
        this.skipBufferSize = skipBufferSize;
    }

    @Override
    public long skip(long n) throws IOException {
        final byte[] buf = new byte[skipBufferSize];
        long total = 0;
        for (long len; 0 < (len = n - total); total += len) {
            len = read(buf, 0, len < buf.length ? (int) len : buf.length);
            if (0 > len) break;
        }
        return total;
    }
}
