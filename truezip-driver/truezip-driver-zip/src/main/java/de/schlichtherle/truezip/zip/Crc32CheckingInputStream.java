/*
 * Copyright (C) 2006-2011 Schlichtherle IT Services
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
