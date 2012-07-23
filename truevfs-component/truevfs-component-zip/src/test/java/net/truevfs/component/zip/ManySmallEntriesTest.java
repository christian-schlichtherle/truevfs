/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.truevfs.component.zip;

import net.truevfs.component.zip.ZipFile;
import net.truevfs.component.zip.ZipOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.Enumeration;
import java.util.HashSet;
import static net.truevfs.component.zip.ZipEntry.STORED;
import net.truevfs.kernel.spec.cio.ByteArrayIoBuffer;
import net.truevfs.kernel.spec.cio.Entry.Size;
import net.truevfs.kernel.spec.cio.IoEntry;
import static net.truevfs.kernel.spec.util.HashMaps.initialCapacity;
import static org.junit.Assert.*;
import org.junit.Test;

/**
 * Tests round trip compression of more than 2^16-1 entries
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
        final IoEntry<?> buffer = new ByteArrayIoBuffer("zip", ZIP_SIZE);
        final byte[] data = DATA_STRING.getBytes(DATA_CHARSET);
        final HashSet<String> set = new HashSet<>(initialCapacity(NUM_ENTRIES));

        try (final ZipOutputStream zos = new ZipOutputStream(
               buffer.output().stream(null))) {
            for (int i = FIRST_ENTRY; i <= LAST_ENTRY; i++) {
                final String name = Integer.toString(i);
                final ZipEntry entry = new ZipEntry(name);

                // Speed up the test a bit.
                entry.setSize(data.length);
                entry.setCompressedSize(data.length);
                entry.setCrc(DATA_CRC);
                entry.setMethod(STORED);

                zos.putNextEntry(entry);
                zos.write(data);
                assertTrue(set.add(name));
            }
        }
        assertEquals(ZIP_SIZE, buffer.getSize(Size.STORAGE));

        try (final ZipFile zf = new ZipFile(buffer.input().channel(null))) {
            final byte[] buf = new byte[data.length];
            for (   final Enumeration<? extends ZipEntry> e = zf.entries();
                    e.hasMoreElements(); ) {
                final ZipEntry entry = e.nextElement();
                final String name = entry.getName();

                assertEquals(data.length, entry.getSize());

                try (final InputStream in = zf.getCheckedInputStream(name)) {
                    int off = 0;
                    int read;
                    do {
                        read = in.read(buf);
                        if (read < 0)
                            break;
                        assertTrue(read > 0);
                        assertEquals(   ByteBuffer.wrap(data, off, read),
                                        ByteBuffer.wrap(buf, 0, read));
                        off += read;
                    } while (true);
                    assertEquals(-1, read);
                    assertEquals(off, data.length);
                    assertEquals(0, in.read(new byte[0]));
                }

                assertTrue(set.remove(name));
            }
        }

        assertTrue(set.isEmpty());
    }
}
