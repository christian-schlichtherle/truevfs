/*
 * Copyright (C) 2011 Schlichtherle IT Services
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

import java.io.File;
import java.util.Random;
import java.io.EOFException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author Christian Schlichtherle
 * @version $Id$
 */
public final class PlainZipTest extends ZipTestSuite {

    private static final Random rnd = new Random();

    @Test
    public final void testAppending() throws IOException {
        // Setup data.
        final byte[] data1 = getData();
        final byte[] data2 = new byte[data1.length];
        rnd.nextBytes(data2);

        // Create and append.
        append(0, 20, data1);
        append(10, 20, data2);

        final File zip = getZip();
        final ZipFile zipIn = newZipFile(zip);
        assertEquals(30, zipIn.size());
        try {
            // Check that zipIn correctly enumerates all entries.
            final byte[] buf = new byte[data1.length];
            for (int i = 0; i < 30; i++) {
                final String name = i + ".txt";
                final ZipEntry entry = zipIn.getEntry(name);
                assertEquals(data1.length, entry.getSize());
                final InputStream in = zipIn.getInputStream(name);
                try {
                    int off = 0;
                    int read;
                    do {
                        read = in.read(buf, off, buf.length - off);
                        if (read < 0)
                            throw new EOFException();
                        assertTrue(read > 0);
                        off += read;
                    } while (off < buf.length);
                    assertEquals(-1, in.read());
                    assertTrue(Arrays.equals(i < 10 ? data1 : data2, buf));
                } finally {
                    in.close();
                }
            }
        } finally {
            zipIn.close();
        }
    }

    private void append(
            final int off,
            final int len,
            final byte[] data)
    throws IOException {
        final File zip = getZip();
        final ZipOutputStream out;
        if (zip.exists()) {
            final ZipFile in = newZipFile(zip);
            in.close();
            out = newZipOutputStream(new FileOutputStream(zip, true), in);
        } else {
            out = newZipOutputStream(new FileOutputStream(zip));
        }
        try {
            for (int i = 0; i < len; i++) {
                final String name = off + i + ".txt";
                out.putNextEntry(new ZipEntry(name));
                out.write(data);
            }
        } finally {
            out.close();
        }
    }
}
