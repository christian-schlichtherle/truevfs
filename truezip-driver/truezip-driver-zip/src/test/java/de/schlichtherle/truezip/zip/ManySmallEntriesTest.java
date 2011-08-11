/*
 * Copyright (C) 2005-2011 Schlichtherle IT Services
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

import de.schlichtherle.truezip.util.ArrayHelper;
import static de.schlichtherle.truezip.zip.ZipEntry.*;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.junit.After;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests compression of data.
 *
 * @author Christian Schlichtherle
 */
public final class ManySmallEntriesTest {

    private static final Logger logger
            = Logger.getLogger(ManySmallEntriesTest.class.getName());

    private static final byte[] data = "Hello World!".getBytes();
    private static final long dataCrc = 0x1c291ca3;

    private File zip;

    @Before
    public void setUp() throws IOException {
        zip = File.createTempFile("zip", null);
    }

    @After
    public void tearDown() {
        assertTrue(zip.delete());
    }

    @Test
    public void testManySmallEntries() throws IOException {
        logger.fine("testManySmallEntries");
        
        final int n = 70000; // > 0xffff;
        logger.log(Level.FINER, "Compressing {0} ZIP file entries to: {1}", new Object[]{ n, zip.getPath() });
        logger.finer("Note that the max. number of entries supported by the ZIP File Format Spec. is 65535!");

        final HashSet<String> set = new HashSet<String>();

        final ZipOutputStream zipOut
                = new ZipOutputStream(new FileOutputStream(zip));
        for (int i = 100000; i < 100000 + n; i++) {
            String name = i + ".txt";
            final ZipEntry entry = new ZipEntry(name);

            // Speed up the test a bit.
            entry.setSize(data.length);
            entry.setCompressedSize(data.length);
            entry.setCrc(dataCrc);
            entry.setMethod(STORED);

            zipOut.putNextEntry(entry);
            zipOut.write(data);
            assertTrue(set.add(name));
        }
        zipOut.close();
        logger.log(Level.FINER, "Compressed {0} ZIP file entries into {1} KB ZIP file length.", new Object[]{ n, zip.length() / 1024 });

        final ZipFile zipIn = new ZipFile(zip);
        try {
            final byte[] buf = new byte[data.length];
            for (Enumeration<? extends ZipEntry> e = zipIn.entries(); e.hasMoreElements(); ) {
                final ZipEntry entry = e.nextElement();

                assertEquals(data.length, entry.getSize());

                InputStream in = zipIn.getInputStream(entry);
                int off = 0;
                int read;
                do {
                    read = in.read(buf);
                    if (read < 0)
                        break;
                    assertTrue(read > 0);
                    assertTrue(ArrayHelper.equals(data, off, buf, 0, read));
                    off += read;
                } while (true);
                assertEquals(-1, read);
                assertEquals(off, data.length);
                assertEquals(0, in.read(new byte[0]));
                in.close();

                assertTrue(set.remove(entry.getName()));
            }
        } finally {
            zipIn.close();
        }

        assertTrue(set.isEmpty());
        logger.finer("Successfully decompressed the data in all entries.");
    }
}
