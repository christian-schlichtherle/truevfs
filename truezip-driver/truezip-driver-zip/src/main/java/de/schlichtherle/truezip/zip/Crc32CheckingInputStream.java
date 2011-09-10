/*
 * Copyright (C) 2006-2011 Schlichtherle IT Services
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package de.schlichtherle.truezip.zip;

import java.io.IOException;
import java.io.InputStream;
import java.util.zip.CRC32;
import java.util.zip.CheckedInputStream;

/**
 * Compares the CRC computed from the content to the CRC in the ZIP entry
 * and throws a CRC32Exception if it detects a mismatch in its method
 * {@link #close()}.
 *
 * @author  Christian Schlichtherle
 * @version $Id$
 */
final class Crc32CheckingInputStream extends CheckedInputStream {
    private final ZipEntry entry;
    private final int size;

    Crc32CheckingInputStream(
            final InputStream in,
            final ZipEntry entry,
            final int size) {
        super(in, new CRC32());
        this.entry = entry;
        this.size = size;
    }

    /**
     * This method skips {@code toSkip} bytes in the given input stream
     * using the given buffer unless EOF or IOException.
     */
    @Override
    public long skip(long toSkip) throws IOException {
        long total = 0;
        final byte[] buf = new byte[size];
        for (long len; (len = toSkip - total) > 0; total += len) {
            len = read(buf, 0, len < buf.length ? (int) len : buf.length);
            if (len < 0) {
                break;
            }
        }
        return total;
    }

    @Override
    public void close() throws IOException {
        while (skip(Long.MAX_VALUE) > 0) {
            // process CRC-32 until EOF
        }
        super.close();
        final long expected = entry.getCrc();
        final long computed = getChecksum().getValue();
        if (expected != computed) {
            throw new CRC32Exception(entry.getName(), expected, computed);
        }
    }
}
