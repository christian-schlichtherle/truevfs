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

import java.util.Arrays;
import org.junit.Before;
import org.junit.Test;
import static org.hamcrest.CoreMatchers.*;
import static de.schlichtherle.truezip.zip.Constants.*;
import static de.schlichtherle.truezip.zip.ZipEntry.*;
import static org.junit.Assert.*;

/**
 * @author Christian Schlichtherle
 * @version $Id$
 */
public final class ZipEntryTest {
    
    private ZipEntry entry;

    @Before
    public void setUp() {
        entry = new ZipEntry("test");
    }

    @Test
    public void testClone() {
        // TODO: Complete this test!
        ZipEntry clone = entry.clone();
        assertNotSame(clone, entry);
    }

    @Test
    public void testPlatform() {
        try {
            entry.setPlatform((short) (UNKNOWN - 1));
            fail("Expected RuntimeException");
        } catch (RuntimeException ex) {
        }

        try {
            entry.setPlatform((short) (0xff + 1));
            fail("Expected RuntimeException");
        } catch (RuntimeException ex) {
        }

        assertEquals(UNKNOWN, entry.getPlatform());
        entry.setPlatform((short) 0x00);
        assertEquals((short) 0x00, entry.getPlatform());
        entry.setPlatform(ZipEntry.PLATFORM_FAT);
        assertEquals(ZipEntry.PLATFORM_FAT, entry.getPlatform());
        entry.setPlatform(ZipEntry.PLATFORM_UNIX);
        assertEquals(ZipEntry.PLATFORM_UNIX, entry.getPlatform());
        entry.setPlatform((short) 0xff);
        assertEquals((short) 0xff, entry.getPlatform());
        entry.setPlatform(UNKNOWN);
        assertEquals(UNKNOWN, entry.getPlatform());
    }

    @Test
    public void testGeneral() {
        try {
            entry.setGeneral(UNKNOWN - 1);
            fail("Expected RuntimeException");
        } catch (RuntimeException ex) {
        }

        try {
            entry.setGeneral(0xffff + 1);
            fail("Expected RuntimeException");
        } catch (RuntimeException ex) {
        }

        assertEquals(UNKNOWN, entry.getGeneral());
        entry.setGeneral(0);
        assertEquals(0, entry.getGeneral());
        entry.setGeneral(8);
        assertEquals(8, entry.getGeneral());
        entry.setGeneral(1 << 11);
        assertEquals(1 << 11, entry.getGeneral());
        entry.setGeneral(0xffff);
        assertEquals(0xffff, entry.getGeneral());
        entry.setGeneral(UNKNOWN);
        assertEquals(UNKNOWN, entry.getGeneral());
    }

    @Test
    public void testGeneralBit() {
        assertThat(entry.getGeneral(), is((int) UNKNOWN));

        try {
            entry.setGeneralBit(-1, false);
            fail();
        } catch (IllegalArgumentException ex) {
        }

        for (int i = 0; i < 16; i++) {
            assertFalse(entry.getGeneralBit(i));
            entry.setGeneralBit(i, true);
            assertTrue(entry.getGeneralBit(i));
        }

        try {
            entry.setGeneralBit(16, false);
            fail();
        } catch (IllegalArgumentException ex) {
        }

        entry.setGeneral(UNKNOWN);
        assertThat(entry.getGeneral(), is((int) UNKNOWN));

        for (int i = 0; i < 16; i++) {
            assertFalse(entry.getGeneralBit(i));
            entry.setGeneralBit(i, true);
            assertTrue(entry.getGeneralBit(i));
        }
    }

    @Test
    public void testMethod() {
        try {
            entry.setMethod(UNKNOWN - 1);
            fail("Expected RuntimeException");
        } catch (RuntimeException ex) {
        }

        try {
            entry.setMethod(0xffff);
            fail("Expected RuntimeException");
        } catch (RuntimeException ex) {
        }

        try {
            entry.setMethod(0xffff + 1);
            fail("Expected RuntimeException");
        } catch (RuntimeException ex) {
        }

        assertEquals(UNKNOWN, entry.getMethod());
        /*entry.setMethod(0);
        assertEquals(0, entry.getMethod());*/
        entry.setMethod(ZipEntry.STORED);
        assertEquals(ZipEntry.STORED, entry.getMethod());
        entry.setMethod(ZipEntry.DEFLATED);
        assertEquals(ZipEntry.DEFLATED, entry.getMethod());
        /*entry.setMethod(0xffff);
        assertEquals(0xffff, entry.getMethod());*/
        entry.setMethod(UNKNOWN);
        assertEquals(UNKNOWN, entry.getMethod());
    }

    @Test
    public void testRoundTripTimeConversion() {
        // Must start with DOS time due to lesser time granularity.
        long dosTime = ZipEntry.MIN_DOS_TIME;
        assertEquals(dosTime, DateTimeConverter.JAR.toDosTime(DateTimeConverter.JAR.toJavaTime(dosTime)));

        dosTime = DateTimeConverter.JAR.toDosTime(System.currentTimeMillis());
        assertEquals(dosTime, DateTimeConverter.JAR.toDosTime(DateTimeConverter.JAR.toJavaTime(dosTime)));
    }

    @Test
    public void testDosTime() {
        entry.setDosTime(UNKNOWN - 1);
        assertEquals(MIN_DOS_TIME, entry.getDosTime());

        entry.setDosTime(Long.MIN_VALUE);
        assertEquals(MIN_DOS_TIME, entry.getDosTime());

        entry.setDosTime(MIN_DOS_TIME - 1);
        assertEquals(MIN_DOS_TIME, entry.getDosTime());

        entry.setDosTime(MIN_DOS_TIME);
        assertEquals(MIN_DOS_TIME, entry.getDosTime());

        entry.setDosTime(MAX_DOS_TIME);
        assertEquals(MAX_DOS_TIME, entry.getDosTime());

        entry.setDosTime(MAX_DOS_TIME + 1);
        assertEquals(MAX_DOS_TIME, entry.getDosTime());

        entry.setDosTime(Long.MAX_VALUE);
        assertEquals(MAX_DOS_TIME, entry.getDosTime());

        entry.setDosTime(UNKNOWN);
        assertEquals(UNKNOWN, entry.getDosTime());
    }

    @Test
    public void testTime() {
        try {
            entry.setTime(Long.MIN_VALUE);
            fail();
        } catch (IllegalArgumentException ex) {
        }

        try {
            entry.setTime(UNKNOWN - 1);
            fail();
        } catch (IllegalArgumentException ex) {
        }

        assertEquals(UNKNOWN, entry.getTime());

        entry.setTime(0);
        assertEquals(entry.getTime(), DateTimeConverter.JAR.toJavaTime(MIN_DOS_TIME));

        entry.setTime(Long.MAX_VALUE);
        assertEquals(entry.getTime(), DateTimeConverter.JAR.toJavaTime(MAX_DOS_TIME));

        entry.setTime(UNKNOWN);
        assertEquals(UNKNOWN, entry.getTime());
    }

    @Test
    public void testCrc() {
        try {
            entry.setCrc(UNKNOWN - 1);
            fail("Expected RuntimeException");
        } catch (RuntimeException ex) {
        }

        try {
            entry.setCrc(UInt.MAX_VALUE + 1);
            fail("Expected RuntimeException");
        } catch (RuntimeException ex) {
        }

        assertEquals(UNKNOWN, entry.getCrc());
        entry.setCrc(0);
        assertEquals(0, entry.getCrc());
        entry.setCrc(UInt.MAX_VALUE);
        assertEquals(UInt.MAX_VALUE, entry.getCrc());
        entry.setCrc(UNKNOWN);
        assertEquals(UNKNOWN, entry.getCrc());
    }

    @Test
    public void testCompressedSize32() {
        try {
            entry.setCompressedSize32(UNKNOWN - 1);
            fail("Expected RuntimeException");
        } catch (RuntimeException ex) {
        }

        try {
            entry.setCompressedSize32(UInt.MAX_VALUE + 1);
            fail("Expected RuntimeException");
        } catch (RuntimeException ex) {
        }

        assertEquals(UNKNOWN, entry.getCompressedSize32());
        entry.setCompressedSize32(0);
        assertEquals(FORCE_ZIP64_EXT ? UInt.MAX_VALUE : 0, entry.getCompressedSize32());
        entry.setCompressedSize32(UInt.MAX_VALUE);
        assertEquals(UInt.MAX_VALUE, entry.getCompressedSize32());
        entry.setCompressedSize32(UNKNOWN);
        assertEquals(UNKNOWN, entry.getCompressedSize32());
    }

    @Test
    public void testCompressedSize() {
        try {
            entry.setCompressedSize(UNKNOWN - 1);
            fail("Expected RuntimeException");
        } catch (RuntimeException ex) {
        }

        assertEquals(UNKNOWN, entry.getCompressedSize());

        entry.setCompressedSize(0);
        assertEquals(0, entry.getCompressedSize());
        entry.setCompressedSize(UInt.MAX_VALUE);
        assertEquals(UInt.MAX_VALUE, entry.getCompressedSize());
        entry.setCompressedSize(UInt.MAX_VALUE + 1); // ZIP64!
        assertEquals(UInt.MAX_VALUE + 1, entry.getCompressedSize());
        entry.setCompressedSize(UNKNOWN);
        assertEquals(UNKNOWN, entry.getCompressedSize());

    }

    @Test
    public void testSize32() {
        try {
            entry.setSize32(UNKNOWN - 1);
            fail("Expected RuntimeException");
        } catch (RuntimeException ex) {
        }

        try {
            entry.setSize32(UInt.MAX_VALUE + 1);
            fail("Expected RuntimeException");
        } catch (RuntimeException ex) {
        }

        assertEquals(UNKNOWN, entry.getSize32());
        entry.setSize32(0);
        assertEquals(FORCE_ZIP64_EXT ? UInt.MAX_VALUE : 0, entry.getSize32());
        entry.setSize32(UInt.MAX_VALUE);
        assertEquals(UInt.MAX_VALUE, entry.getSize32());
        entry.setSize32(UNKNOWN);
        assertEquals(UNKNOWN, entry.getSize32());
    }

    @Test
    public void testSize() {
        try {
            entry.setSize(UNKNOWN - 1);
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException ex) {
        }

        assertEquals(UNKNOWN, entry.getSize());
        entry.setSize(0);
        assertEquals(0, entry.getSize());
        entry.setSize(UInt.MAX_VALUE);
        assertEquals(UInt.MAX_VALUE, entry.getSize());
        entry.setSize(UInt.MAX_VALUE + 1); // ZIP64!
        assertEquals(UInt.MAX_VALUE + 1, entry.getSize());
        entry.setSize(UNKNOWN);
        assertEquals(UNKNOWN, entry.getSize());
    }

    @Test
    public void testOffset32() {
        try {
            entry.setOffset32(UNKNOWN - 1);
            fail("Expected RuntimeException");
        } catch (RuntimeException ex) {
        }

        try {
            entry.setOffset32(UInt.MAX_VALUE + 1);
            fail("Expected RuntimeException");
        } catch (RuntimeException ex) {
        }

        assertEquals(UNKNOWN, entry.getOffset32());
        entry.setOffset32(0);
        assertEquals(FORCE_ZIP64_EXT ? UInt.MAX_VALUE : 0, entry.getOffset32());
        entry.setOffset32(UInt.MAX_VALUE);
        assertEquals(UInt.MAX_VALUE, entry.getOffset32());
        entry.setOffset32(UNKNOWN);
        assertEquals(UNKNOWN, entry.getOffset32());
    }

    @Test
    public void testOffset() {
        try {
            entry.setOffset(UNKNOWN - 1);
            fail("Expected RuntimeException");
        } catch (RuntimeException ex) {
        }

        assertEquals(UNKNOWN, entry.getOffset());
        entry.setOffset(0);
        assertEquals(0, entry.getOffset());
        entry.setOffset(UInt.MAX_VALUE);
        assertEquals(UInt.MAX_VALUE, entry.getOffset());
        entry.setOffset(UInt.MAX_VALUE + 1); // ZIP64!
        assertEquals(UInt.MAX_VALUE + 1, entry.getOffset());
        entry.setOffset(UNKNOWN);
        assertEquals(UNKNOWN, entry.getOffset());
    }

    @Test
    public void testExtra() {
        assertEquals(0, entry.getExtra().length);

        // Serialized Extra Fields in little endian order.
        final byte[] set = new byte[] {
            (byte) 0x00, (byte) 0x00, // Header ID: 0x0000 (undefined)
            (byte) 0x00, (byte) 0x00, // Data Size: 0x0000
            (byte) 0x01, (byte) 0x00, // Header ID: 0x0001 (Zip64)
            (byte) 0x18, (byte) 0x00, // Data Size: 0x0018 (= 3 * 8)
            (byte) 0x21, (byte) 0x43, (byte) 0x65, (byte) 0x87, // Original Size
            (byte) 0xa9, (byte) 0xcb, (byte) 0xed, (byte) 0x0f,
            (byte) 0x22, (byte) 0x43, (byte) 0x65, (byte) 0x87, // Compressed Size
            (byte) 0xa9, (byte) 0xcb, (byte) 0xed, (byte) 0x0f,
            (byte) 0x23, (byte) 0x43, (byte) 0x65, (byte) 0x87, // Relative Header Offset
            (byte) 0xa9, (byte) 0xcb, (byte) 0xed, (byte) 0x0f,
            (byte) 0xfe, (byte) 0xca, // Header ID: 0xcafe (JarMarker)
            (byte) 0x00, (byte) 0x00, // Data Size: 0x0000
        };

        entry.setSize32(UInt.MAX_VALUE);
        entry.setCompressedSize32(UInt.MAX_VALUE);
        entry.setOffset32(UInt.MAX_VALUE);
        entry.setExtra(set); // this must be last in the sequence!
        assertEquals(0x0fedcba987654321L, entry.getSize());
        assertEquals(0x0fedcba987654322L, entry.getCompressedSize());
        assertEquals(0x0fedcba987654323L, entry.getOffset());

        set[0] = (byte) 0xff;

        byte[] got1 = entry.getExtra();
        assertNotNull(got1);
        assertNotSame(set, got1);

        final byte[] got2 = entry.getExtra();
        assertNotNull(got2);
        assertNotSame(set, got2);

        assertNotSame(got1, got2);

        set[0] = (byte) 0x00;

        assertTrue(Arrays.equals(set, got1));
        assertTrue(Arrays.equals(set, got2));
    }
}
