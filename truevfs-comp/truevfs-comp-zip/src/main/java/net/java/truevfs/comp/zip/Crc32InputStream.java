/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.comp.zip;

import java.io.IOException;
import java.io.InputStream;
import java.util.zip.CRC32;

/**
 * Compares the CRC computed from the content to the CRC in the ZIP entry
 * and throws a Crc32Exception if it detects a mismatch in its method
 * {@link #close()}.
 *
 * @author Christian Schlichtherle
 */
final class Crc32InputStream extends CheckedInputStream {

    private final ZipEntry entry;
    private boolean closed;

    Crc32InputStream(
            final InputStream in,
            final int skipBufferSize,
            final ZipEntry entry) {
        super(in, new CRC32(), skipBufferSize);
        this.entry = entry;
    }

    @Override
    public void close() throws IOException {
        if (!closed) {
            // process CRC-32 until EOF.
            while (skip(Long.MAX_VALUE) > 0) {
            }
        }
        super.close();
        closed = true;
        final long expected = entry.getCrc();
        final long computed = getChecksum().getValue();
        if (expected != computed)
            throw new Crc32Exception(entry.getName(), expected, computed);
    }
}
