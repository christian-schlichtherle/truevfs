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

package de.schlichtherle.truezip.io.zip;

import java.util.Arrays;
import junit.framework.TestCase;

import static de.schlichtherle.truezip.io.zip.ZipConstants.*;

/**
 * @author Christian Schlichtherle
 * @version $Id$
 */
public class ZipEntryTest extends TestCase {
    
    private ZipEntry entry;
    
    public ZipEntryTest(String testName) {
        super(testName);
    }

    @Override
    protected void setUp()
            throws Exception {
        super.setUp();
        entry = new ZipEntry("test");
    }

    @Override
    protected void tearDown() throws Exception {
        entry = null;
        super.tearDown();
    }

    public void testClone() {
        // TODO: Complete this test!
        ZipEntry clone = entry.clone();
        assertNotSame(clone, entry);
    }

    public void testPlatform() {
        try {
            entry.setPlatform((short) (ZipEntry.UNKNOWN - 1));
            fail("Expected RuntimeException");
        } catch (RuntimeException ex) {
        }

        try {
            entry.setPlatform((short) (0xff + 1));
            fail("Expected RuntimeException");
        } catch (RuntimeException ex) {
        }

        assertEquals(ZipEntry.UNKNOWN, entry.getPlatform());
        entry.setPlatform((short) 0x00);
        assertEquals((short) 0x00, entry.getPlatform());
        entry.setPlatform(ZipEntry.PLATFORM_FAT);
        assertEquals(ZipEntry.PLATFORM_FAT, entry.getPlatform());
        entry.setPlatform(ZipEntry.PLATFORM_UNIX);
        assertEquals(ZipEntry.PLATFORM_UNIX, entry.getPlatform());
        entry.setPlatform((short) 0xff);
        assertEquals((short) 0xff, entry.getPlatform());
        entry.setPlatform(ZipEntry.UNKNOWN);
        assertEquals(ZipEntry.UNKNOWN, entry.getPlatform());
    }

    public void testGeneral() {
        try {
            entry.setGeneral(ZipEntry.UNKNOWN - 1);
            fail("Expected RuntimeException");
        } catch (RuntimeException ex) {
        }

        try {
            entry.setGeneral(0xffff + 1);
            fail("Expected RuntimeException");
        } catch (RuntimeException ex) {
        }

        assertEquals(ZipEntry.UNKNOWN, entry.getGeneral());
        entry.setGeneral(0);
        assertEquals(0, entry.getGeneral());
        entry.setGeneral(8);
        assertEquals(8, entry.getGeneral());
        entry.setGeneral(1 << 11);
        assertEquals(1 << 11, entry.getGeneral());
        entry.setGeneral(0xffff);
        assertEquals(0xffff, entry.getGeneral());
        entry.setGeneral(ZipEntry.UNKNOWN);
        assertEquals(ZipEntry.UNKNOWN, entry.getGeneral());
    }

    public void testGeneralBit() {
        for (int i = -1; i < 17; i++) {
            try {
                entry.getGeneralBit(i);
                fail("Expected IllegalStateException");
            } catch (IllegalStateException ex) {
            }
        }

        try {
            entry.setGeneralBit(-1, false);
            fail("Expected RuntimeException");
        } catch (RuntimeException ex) {
        }

        for (int i = 0; i < 16; i++) {
            entry.setGeneralBit(i, false);
            assertFalse(entry.getGeneralBit(i));
            entry.setGeneralBit(i, true);
            assertTrue(entry.getGeneralBit(i));
        }

        try {
            entry.setGeneralBit(16, false);
            fail("Expected RuntimeException");
        } catch (RuntimeException ex) {
        }

        entry.setGeneral(ZipEntry.UNKNOWN);
        try {
            entry.getGeneralBit(0);
            fail("Expected IllegalStateException");
        } catch (IllegalStateException ex) {
        }
    }

    public void testMethod() {
        try {
            entry.setMethod(ZipEntry.UNKNOWN - 1);
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

        assertEquals(ZipEntry.UNKNOWN, entry.getMethod());
        /*entry.setMethod(0);
        assertEquals(0, entry.getMethod());*/
        entry.setMethod(ZipEntry.STORED);
        assertEquals(ZipEntry.STORED, entry.getMethod());
        entry.setMethod(ZipEntry.DEFLATED);
        assertEquals(ZipEntry.DEFLATED, entry.getMethod());
        /*entry.setMethod(0xffff);
        assertEquals(0xffff, entry.getMethod());*/
        entry.setMethod(ZipEntry.UNKNOWN);
        assertEquals(ZipEntry.UNKNOWN, entry.getMethod());
    }

    public void testRoundTripTimeConversion() {
        // Must start with DOS time due to lesser time granularity.
        long dosTime = ZipEntry.MIN_DOS_TIME;
        assertEquals(dosTime, DateTimeConverter.JAR.toDosTime(DateTimeConverter.JAR.toJavaTime(dosTime)));

        dosTime = DateTimeConverter.JAR.toDosTime(System.currentTimeMillis());
        assertEquals(dosTime, DateTimeConverter.JAR.toDosTime(DateTimeConverter.JAR.toJavaTime(dosTime)));
    }

    public void testDosTime() {
        try {
            entry.setDosTime(ZipEntry.UNKNOWN - 1);
            fail("Expected RuntimeException");
        } catch (RuntimeException ex) {
        }

        try {
            entry.setDosTime(0);
            fail("Expected RuntimeException");
        } catch (RuntimeException ex) {
        }

        try {
            entry.setDosTime(UInt.MAX_VALUE + 1);
            fail("Expected RuntimeException");
        } catch (RuntimeException ex) {
        }

        assertEquals(ZipEntry.UNKNOWN, entry.getDosTime());

        entry.setDosTime(ZipEntry.MIN_DOS_TIME);
        assertEquals(ZipEntry.MIN_DOS_TIME, entry.getDosTime());

        entry.setDosTime(ZipEntry.UNKNOWN);
        assertEquals(ZipEntry.UNKNOWN, entry.getDosTime());
    }

    public void testTime() {
        try {
            entry.setTime(ZipEntry.UNKNOWN - 1);
            fail("Expected RuntimeException");
        } catch (RuntimeException ex) {
        }

        assertEquals(ZipEntry.UNKNOWN, entry.getTime());
        entry.setTime(0);
        long time = entry.getTime();
        assertEquals(time, DateTimeConverter.JAR.toJavaTime(ZipEntry.MIN_DOS_TIME));
        entry.setTime(ZipEntry.UNKNOWN);
        assertEquals(ZipEntry.UNKNOWN, entry.getTime());
    }

    public void testCrc() {
        try {
            entry.setCrc(ZipEntry.UNKNOWN - 1);
            fail("Expected RuntimeException");
        } catch (RuntimeException ex) {
        }

        try {
            entry.setCrc(UInt.MAX_VALUE + 1);
            fail("Expected RuntimeException");
        } catch (RuntimeException ex) {
        }

        assertEquals(ZipEntry.UNKNOWN, entry.getCrc());
        entry.setCrc(0);
        assertEquals(0, entry.getCrc());
        entry.setCrc(UInt.MAX_VALUE);
        assertEquals(UInt.MAX_VALUE, entry.getCrc());
        entry.setCrc(ZipEntry.UNKNOWN);
        assertEquals(ZipEntry.UNKNOWN, entry.getCrc());
    }

    public void testCompressedSize32() {
        try {
            entry.setCompressedSize32(ZipEntry.UNKNOWN - 1);
            fail("Expected RuntimeException");
        } catch (RuntimeException ex) {
        }

        try {
            entry.setCompressedSize32(UInt.MAX_VALUE + 1);
            fail("Expected RuntimeException");
        } catch (RuntimeException ex) {
        }

        assertEquals(ZipEntry.UNKNOWN, entry.getCompressedSize32());
        entry.setCompressedSize32(0);
        assertEquals(ZIP64_EXT ? UInt.MAX_VALUE : 0, entry.getCompressedSize32());
        entry.setCompressedSize32(UInt.MAX_VALUE);
        assertEquals(UInt.MAX_VALUE, entry.getCompressedSize32());
        entry.setCompressedSize32(ZipEntry.UNKNOWN);
        assertEquals(ZipEntry.UNKNOWN, entry.getCompressedSize32());
    }

    public void testCompressedSize() {
        try {
            entry.setCompressedSize(ZipEntry.UNKNOWN - 1);
            fail("Expected RuntimeException");
        } catch (RuntimeException ex) {
        }

        assertEquals(ZipEntry.UNKNOWN, entry.getCompressedSize());

        entry.setCompressedSize(0);
        assertEquals(0, entry.getCompressedSize());
        entry.setCompressedSize(UInt.MAX_VALUE);
        assertEquals(UInt.MAX_VALUE, entry.getCompressedSize());
        entry.setCompressedSize(UInt.MAX_VALUE + 1); // ZIP64!
        assertEquals(UInt.MAX_VALUE + 1, entry.getCompressedSize());
        entry.setCompressedSize(ZipEntry.UNKNOWN);
        assertEquals(ZipEntry.UNKNOWN, entry.getCompressedSize());

    }

    public void testSize32() {
        try {
            entry.setSize32(ZipEntry.UNKNOWN - 1);
            fail("Expected RuntimeException");
        } catch (RuntimeException ex) {
        }

        try {
            entry.setSize32(UInt.MAX_VALUE + 1);
            fail("Expected RuntimeException");
        } catch (RuntimeException ex) {
        }

        assertEquals(ZipEntry.UNKNOWN, entry.getSize32());
        entry.setSize32(0);
        assertEquals(ZIP64_EXT ? UInt.MAX_VALUE : 0, entry.getSize32());
        entry.setSize32(UInt.MAX_VALUE);
        assertEquals(UInt.MAX_VALUE, entry.getSize32());
        entry.setSize32(ZipEntry.UNKNOWN);
        assertEquals(ZipEntry.UNKNOWN, entry.getSize32());
    }

    public void testSize() {
        try {
            entry.setSize(ZipEntry.UNKNOWN - 1);
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException ex) {
        }

        assertEquals(ZipEntry.UNKNOWN, entry.getSize());
        entry.setSize(0);
        assertEquals(0, entry.getSize());
        entry.setSize(UInt.MAX_VALUE);
        assertEquals(UInt.MAX_VALUE, entry.getSize());
        entry.setSize(UInt.MAX_VALUE + 1); // ZIP64!
        assertEquals(UInt.MAX_VALUE + 1, entry.getSize());
        entry.setSize(ZipEntry.UNKNOWN);
        assertEquals(ZipEntry.UNKNOWN, entry.getSize());
    }

    public void testOffset32() {
        try {
            entry.setOffset32(ZipEntry.UNKNOWN - 1);
            fail("Expected RuntimeException");
        } catch (RuntimeException ex) {
        }

        try {
            entry.setOffset32(UInt.MAX_VALUE + 1);
            fail("Expected RuntimeException");
        } catch (RuntimeException ex) {
        }

        assertEquals(ZipEntry.UNKNOWN, entry.getOffset32());
        entry.setOffset32(0);
        assertEquals(ZIP64_EXT ? UInt.MAX_VALUE : 0, entry.getOffset32());
        entry.setOffset32(UInt.MAX_VALUE);
        assertEquals(UInt.MAX_VALUE, entry.getOffset32());
        entry.setOffset32(ZipEntry.UNKNOWN);
        assertEquals(ZipEntry.UNKNOWN, entry.getOffset32());
    }

    public void testOffset() {
        try {
            entry.setOffset(ZipEntry.UNKNOWN - 1);
            fail("Expected RuntimeException");
        } catch (RuntimeException ex) {
        }

        assertEquals(ZipEntry.UNKNOWN, entry.getOffset());
        entry.setOffset(0);
        assertEquals(0, entry.getOffset());
        entry.setOffset(UInt.MAX_VALUE);
        assertEquals(UInt.MAX_VALUE, entry.getOffset());
        entry.setOffset(UInt.MAX_VALUE + 1); // ZIP64!
        assertEquals(UInt.MAX_VALUE + 1, entry.getOffset());
        entry.setOffset(ZipEntry.UNKNOWN);
        assertEquals(ZipEntry.UNKNOWN, entry.getOffset());
    }

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
