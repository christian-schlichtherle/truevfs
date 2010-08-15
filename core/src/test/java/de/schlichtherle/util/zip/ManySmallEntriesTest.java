/*
 * Copyright (C) 2005-2010 Schlichtherle IT Services
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

package de.schlichtherle.util.zip;

import de.schlichtherle.util.Arrays;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.logging.Logger;

import junit.framework.*;

/**
 * Tests compression of data.
 *
 * @author Christian Schlichtherle
 */
public class ManySmallEntriesTest extends TestCase {

    private static final Logger logger
            = Logger.getLogger(ManySmallEntriesTest.class.getName());

    private static final byte[] data = "Hello World!".getBytes();

    public static Test suite() throws Exception {
        TestSuite suite = new TestSuite(ManySmallEntriesTest.class);
        
        return suite;
    }

    private File zip;

    public ManySmallEntriesTest(String testName) {
        super(testName);
    }

    protected void setUp() throws IOException {
        zip = File.createTempFile("zip", null);
    }

    protected void tearDown() {
        assertTrue(zip.delete());
    }

    public void testManySmallEntries() throws IOException {
        logger.fine("testManySmallEntries");
        
        final int n = 70000; // 0xffff;
        logger.finer("Compressing " + n + " ZIP file entries to: " + zip.getPath());
        logger.finer("Note that the max. number of entries supported by the ZIP File Format Spec. is 65535!");

        final HashSet set = new HashSet();

        final ZipOutputStream zipOut
                = new ZipOutputStream(new FileOutputStream(zip));
        for (int i = 100000; i < 100000 + n; i++) {
            String name = i + ".txt";
            zipOut.putNextEntry(new ZipEntry(name));
            zipOut.write(data);
            assertTrue(set.add(name));
        }
        zipOut.close();
        logger.finer("Compressed " + n + " ZIP file entries into " + zip.length() / 1024 + " KB ZIP file length.");

        final ZipFile zipIn = new ZipFile(zip);
        try {
            final byte[] buf = new byte[data.length];
            for (Enumeration e = zipIn.entries(); e.hasMoreElements(); ) {
                final ZipEntry entry = (ZipEntry) e.nextElement();

                assertEquals(data.length, entry.getSize());

                InputStream in = zipIn.getInputStream(entry);
                int off = 0;
                int read;
                do {
                    read = in.read(buf);
                    if (read < 0)
                        break;
                    assertTrue(read > 0);
                    assertTrue(Arrays.equals(data, off, buf, 0, read));
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
