/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.truezip.driver.zip.io;

import static de.truezip.driver.zip.io.ZipEntry.STORED;
import de.truezip.kernel.cio.ByteArrayIOBuffer;
import de.truezip.kernel.cio.Entry.Size;
import de.truezip.kernel.cio.IOEntry;
import de.truezip.kernel.util.ArrayUtils;
import de.truezip.kernel.util.Maps;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.Enumeration;
import java.util.HashSet;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

/**
 * Tests compression of data.
 *
 * @author Christian Schlichtherle
 */
public final class ManySmallEntriesTest {

    private static final int FIRST_ENTRY = 100000;
    private static final int LAST_ENTRY  = 169999;
    private static final int NUM_ENTRIES = LAST_ENTRY - FIRST_ENTRY + 1;
    static { assert NUM_ENTRIES > 65535; /* 0xffff */ }

    private static final String DATA_STRING = "Hello World!";
    private static final Charset DATA_CHARSET = Charset.forName("US-ASCII");
    private static final int DATA_CRC = 0x1c291ca3; // pre-computed
    private static final int ZIP_SIZE = 7000098; // pre-computed

    @Test
    public void testManySmallEntries() throws IOException {
        final IOEntry<?> buffer = new ByteArrayIOBuffer("zip", ZIP_SIZE);
        final byte[] data = DATA_STRING.getBytes(DATA_CHARSET);
        final HashSet<String> set = new HashSet<>(Maps.initialCapacity(NUM_ENTRIES));

        try (final ZipOutputStream zipOut = new ZipOutputStream(
               buffer.getOutputSocket().newStream())) {
            for (int i = FIRST_ENTRY; i <= LAST_ENTRY; i++) {
                final String name = Integer.toString(i);
                final ZipEntry entry = new ZipEntry(name);

                // Speed up the test a bit.
                entry.setSize(data.length);
                entry.setCompressedSize(data.length);
                entry.setCrc(DATA_CRC);
                entry.setMethod(STORED);

                zipOut.putNextEntry(entry);
                zipOut.write(data);
                assertTrue(set.add(name));
            }
        }
        assertEquals(ZIP_SIZE, buffer.getSize(Size.STORAGE));

        try (final ZipFile zipIn = new ZipFile(buffer.getInputSocket().newReadOnlyFile())) {
            final byte[] buf = new byte[data.length];
            for (final Enumeration<? extends ZipEntry> e = zipIn.entries(); e.hasMoreElements(); ) {
                final ZipEntry entry = e.nextElement();

                assertEquals(data.length, entry.getSize());

                try (final InputStream in = zipIn.getCheckedInputStream(entry)) {
                    int off = 0;
                    int read;
                    do {
                        read = in.read(buf);
                        if (read < 0)
                            break;
                        assertTrue(read > 0);
                        assertTrue(ArrayUtils.equals(data, off, buf, 0, read));
                        off += read;
                    } while (true);
                    assertEquals(-1, read);
                    assertEquals(off, data.length);
                    assertEquals(0, in.read(new byte[0]));
                }

                assertTrue(set.remove(entry.getName()));
            }
        }

        assertTrue(set.isEmpty());
    }
}
