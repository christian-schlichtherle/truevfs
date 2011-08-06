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
            fail();
        } catch (IllegalArgumentException expected) {
        }

        try {
            entry.setPlatform((short) (UByte.MAX_VALUE + 1));
            fail();
        } catch (IllegalArgumentException expected) {
        }

        assertEquals(UNKNOWN, entry.getPlatform());
        entry.setPlatform(UByte.MIN_VALUE);
        assertEquals(UByte.MIN_VALUE, entry.getPlatform());
        entry.setPlatform(PLATFORM_FAT);
        assertEquals(PLATFORM_FAT, entry.getPlatform());
        entry.setPlatform(PLATFORM_UNIX);
        assertEquals(PLATFORM_UNIX, entry.getPlatform());
        entry.setPlatform(UByte.MAX_VALUE);
        assertEquals(UByte.MAX_VALUE, entry.getPlatform());
        entry.setPlatform(UNKNOWN);
        assertEquals(UNKNOWN, entry.getPlatform());
    }

    @Test
    public void testPlatform8() {
        try {
            entry.setPlatform8(UNKNOWN - 1);
            fail();
        } catch (IllegalArgumentException expected) {
        }

        try {
            entry.setPlatform8(UByte.MIN_VALUE - 1);
            fail();
        } catch (IllegalArgumentException expected) {
        }

        try {
            entry.setPlatform8(UByte.MAX_VALUE + 1);
            fail();
        } catch (IllegalArgumentException expected) {
        }

        assertEquals(UNKNOWN, entry.getPlatform());
        entry.setPlatform8(UByte.MIN_VALUE);
        assertEquals(UByte.MIN_VALUE, entry.getPlatform());
        entry.setPlatform8(PLATFORM_FAT);
        assertEquals(PLATFORM_FAT, entry.getPlatform());
        entry.setPlatform8(PLATFORM_UNIX);
        assertEquals(PLATFORM_UNIX, entry.getPlatform());
        entry.setPlatform8(UByte.MAX_VALUE);
        assertEquals(UByte.MAX_VALUE, entry.getPlatform());
    }

    @Test
    public void testGeneral16() {
        try {
            entry.setGeneral16(UNKNOWN - 1);
            fail();
        } catch (IllegalArgumentException expected) {
        }

        try {
            entry.setGeneral16(UShort.MIN_VALUE - 1);
            fail();
        } catch (IllegalArgumentException expected) {
        }

        try {
            entry.setGeneral16(UShort.MAX_VALUE + 1);
            fail();
        } catch (IllegalArgumentException expected) {
        }

        assertEquals(UShort.MIN_VALUE, entry.getGeneral16());
        entry.setGeneral16(1 << GPBF_ENCRYPTED);
        assertEquals(1 << GPBF_ENCRYPTED, entry.getGeneral16());
        entry.setGeneral16(1 << GPBF_DATA_DESCRIPTOR);
        assertEquals(1 << GPBF_DATA_DESCRIPTOR, entry.getGeneral16());
        entry.setGeneral16(1 << GPBF_UTF8);
        assertEquals(1 << GPBF_UTF8, entry.getGeneral16());
        entry.setGeneral16(UShort.MAX_VALUE);
        assertEquals(UShort.MAX_VALUE, entry.getGeneral16());
    }

    @Test
    public void testGeneral1() {
        assertThat(entry.getGeneral16(), is(UShort.MIN_VALUE));

        try {
            entry.setGeneral1(-1, false);
            fail();
        } catch (AssertionError expected) {
        }

        try {
            entry.setGeneral1(16, false);
            fail();
        } catch (AssertionError expected) {
        }

        assertThat(entry.getGeneral16(), is(UShort.MIN_VALUE));

        for (int i = 0; i < 16; i++) {
            assertFalse(entry.getGeneral1(i));
            entry.setGeneral1(i, true);
            assertTrue(entry.getGeneral1(i));
        }
        assertThat(entry.getGeneral16(), is(UShort.MAX_VALUE));

        for (int i = 0; i < 16; i++) {
            entry.setGeneral1(i, false);
            assertFalse(entry.getGeneral1(i));
        }
        assertThat(entry.getGeneral16(), is(UShort.MIN_VALUE));
    }

    @Test
    public void testMethod() {
        try {
            entry.setMethod(UNKNOWN - 1);
            fail();
        } catch (IllegalArgumentException expected) {
        }

        try {
            entry.setMethod(UShort.MAX_VALUE);
            fail();
        } catch (IllegalArgumentException expected) {
        }

        try {
            entry.setMethod(UShort.MAX_VALUE + 1);
            fail();
        } catch (IllegalArgumentException expected) {
        }

        assertEquals(UNKNOWN, entry.getMethod());
        entry.setMethod(STORED);
        assertEquals(STORED, entry.getMethod());
        entry.setMethod(DEFLATED);
        assertEquals(DEFLATED, entry.getMethod());
        entry.setMethod(UNKNOWN);
        assertEquals(UNKNOWN, entry.getMethod());
    }

    @Test
    public void testMethod16() {
        try {
            entry.setMethod16(UNKNOWN - 1);
            fail();
        } catch (IllegalArgumentException expected) {
        }

        try {
            entry.setMethod16(UShort.MIN_VALUE - 1);
            fail();
        } catch (IllegalArgumentException expected) {
        }

        try {
            entry.setMethod16(UShort.MAX_VALUE + 1);
            fail();
        } catch (IllegalArgumentException expected) {
        }

        assertEquals(UNKNOWN, entry.getMethod());
        entry.setMethod16(UShort.MIN_VALUE);
        assertEquals(UShort.MIN_VALUE, entry.getMethod());
        entry.setMethod16(STORED);
        assertEquals(STORED, entry.getMethod());
        entry.setMethod16(DEFLATED);
        assertEquals(DEFLATED, entry.getMethod());
        entry.setMethod16(UShort.MAX_VALUE);
        assertEquals(UShort.MAX_VALUE, entry.getMethod());
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
        entry.setTimeDos(UNKNOWN - 1);
        assertEquals(MIN_DOS_TIME, entry.getTimeDos());

        entry.setTimeDos(Long.MIN_VALUE);
        assertEquals(MIN_DOS_TIME, entry.getTimeDos());

        entry.setTimeDos(MIN_DOS_TIME - 1);
        assertEquals(MIN_DOS_TIME, entry.getTimeDos());

        entry.setTimeDos(MIN_DOS_TIME);
        assertEquals(MIN_DOS_TIME, entry.getTimeDos());

        entry.setTimeDos(MAX_DOS_TIME);
        assertEquals(MAX_DOS_TIME, entry.getTimeDos());

        entry.setTimeDos(MAX_DOS_TIME + 1);
        assertEquals(MAX_DOS_TIME, entry.getTimeDos());

        entry.setTimeDos(Long.MAX_VALUE);
        assertEquals(MAX_DOS_TIME, entry.getTimeDos());

        entry.setTimeDos(UNKNOWN);
        assertEquals(UNKNOWN, entry.getTimeDos());
    }

    @Test
    public void testTime() {
        try {
            entry.setTime(Long.MIN_VALUE);
            fail();
        } catch (IllegalArgumentException expected) {
        }

        try {
            entry.setTime(UNKNOWN - 1);
            fail();
        } catch (IllegalArgumentException expected) {
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
            fail();
        } catch (IllegalArgumentException expected) {
        }

        try {
            entry.setCrc(UInt.MAX_VALUE + 1);
            fail();
        } catch (IllegalArgumentException expected) {
        }

        assertEquals(UNKNOWN, entry.getCrc());
        entry.setCrc(UInt.MIN_VALUE);
        assertEquals(UInt.MIN_VALUE, entry.getCrc());
        entry.setCrc(UInt.MAX_VALUE);
        assertEquals(UInt.MAX_VALUE, entry.getCrc());
        entry.setCrc(UNKNOWN);
        assertEquals(UNKNOWN, entry.getCrc());
    }

    @Test
    public void testCrc32() {
        try {
            entry.setCrc32(UNKNOWN - 1);
            fail();
        } catch (IllegalArgumentException expected) {
        }

        try {
            entry.setCrc32(UInt.MIN_VALUE - 1);
            fail();
        } catch (IllegalArgumentException expected) {
        }

        try {
            entry.setCrc32(UInt.MAX_VALUE + 1);
            fail();
        } catch (IllegalArgumentException expected) {
        }

        assertEquals(UNKNOWN, entry.getCrc());
        entry.setCrc32(UInt.MIN_VALUE);
        assertEquals(UInt.MIN_VALUE, entry.getCrc());
        entry.setCrc32(UInt.MAX_VALUE);
        assertEquals(UInt.MAX_VALUE, entry.getCrc());
    }

    @Test
    public void testCompressedSize() {
        try {
            entry.setCompressedSize(UNKNOWN - 1);
            fail();
        } catch (IllegalArgumentException expected) {
        }

        try {
            entry.setCompressedSize(ULong.MAX_VALUE + 1);
            fail();
        } catch (IllegalArgumentException expected) {
        }

        assertEquals(UNKNOWN, entry.getCompressedSize());
        assertEquals(UNKNOWN, entry.getCompressedSize32());

        entry.setCompressedSize(UInt.MIN_VALUE);
        assertEquals(UInt.MIN_VALUE, entry.getCompressedSize());
        assertEquals(UInt.MIN_VALUE, entry.getCompressedSize32());
        entry.setCompressedSize(UInt.MAX_VALUE);
        assertEquals(UInt.MAX_VALUE, entry.getCompressedSize());
        assertEquals(UInt.MAX_VALUE, entry.getCompressedSize32());
        entry.setCompressedSize(UInt.MAX_VALUE + 1); // ZIP64!
        assertEquals(UInt.MAX_VALUE + 1, entry.getCompressedSize());
        assertEquals(UInt.MAX_VALUE, entry.getCompressedSize32());
        entry.setCompressedSize(UNKNOWN);
        assertEquals(UNKNOWN, entry.getCompressedSize());
        assertEquals(UNKNOWN, entry.getCompressedSize32());
    }

    @Test
    public void testCompressedSize64() {
        try {
            entry.setCompressedSize64(UNKNOWN - 1);
            fail();
        } catch (IllegalArgumentException expected) {
        }

        try {
            entry.setCompressedSize64(ULong.MIN_VALUE - 1);
            fail();
        } catch (IllegalArgumentException expected) {
        }

        try {
            entry.setCompressedSize64(ULong.MAX_VALUE + 1);
            fail();
        } catch (IllegalArgumentException expected) {
        }

        assertEquals(UNKNOWN, entry.getCompressedSize());
        assertEquals(UNKNOWN, entry.getCompressedSize32());

        entry.setCompressedSize64(UInt.MIN_VALUE);
        assertEquals(UInt.MIN_VALUE, entry.getCompressedSize());
        assertEquals(UInt.MIN_VALUE, entry.getCompressedSize32());
        entry.setCompressedSize64(UInt.MAX_VALUE);
        assertEquals(UInt.MAX_VALUE, entry.getCompressedSize());
        assertEquals(UInt.MAX_VALUE, entry.getCompressedSize32());
        entry.setCompressedSize64(UInt.MAX_VALUE + 1); // ZIP64!
        assertEquals(UInt.MAX_VALUE + 1, entry.getCompressedSize());
        assertEquals(UInt.MAX_VALUE, entry.getCompressedSize32());
    }

    @Test
    public void testSize() {
        try {
            entry.setSize(UNKNOWN - 1);
            fail();
        } catch (IllegalArgumentException expected) {
        }

        try {
            entry.setSize(ULong.MAX_VALUE + 1);
            fail();
        } catch (IllegalArgumentException expected) {
        }

        assertEquals(UNKNOWN, entry.getSize());
        assertEquals(UNKNOWN, entry.getSize32());

        entry.setSize(UInt.MIN_VALUE);
        assertEquals(UInt.MIN_VALUE, entry.getSize());
        assertEquals(UInt.MIN_VALUE, entry.getSize32());
        entry.setSize(UInt.MAX_VALUE);
        assertEquals(UInt.MAX_VALUE, entry.getSize());
        assertEquals(UInt.MAX_VALUE, entry.getSize32());
        entry.setSize(UInt.MAX_VALUE + 1); // ZIP64!
        assertEquals(UInt.MAX_VALUE + 1, entry.getSize());
        assertEquals(UInt.MAX_VALUE, entry.getSize32());
        entry.setSize(UNKNOWN);
        assertEquals(UNKNOWN, entry.getSize());
        assertEquals(UNKNOWN, entry.getSize32());
    }

    @Test
    public void testSize64() {
        try {
            entry.setSize64(UNKNOWN - 1);
            fail();
        } catch (IllegalArgumentException expected) {
        }

        try {
            entry.setSize64(ULong.MIN_VALUE - 1);
            fail();
        } catch (IllegalArgumentException expected) {
        }

        try {
            entry.setSize64(ULong.MAX_VALUE + 1);
            fail();
        } catch (IllegalArgumentException expected) {
        }

        assertEquals(UNKNOWN, entry.getSize());
        assertEquals(UNKNOWN, entry.getSize32());

        entry.setSize64(UInt.MIN_VALUE);
        assertEquals(UInt.MIN_VALUE, entry.getSize());
        assertEquals(UInt.MIN_VALUE, entry.getSize32());
        entry.setSize64(UInt.MAX_VALUE);
        assertEquals(UInt.MAX_VALUE, entry.getSize());
        assertEquals(UInt.MAX_VALUE, entry.getSize32());
        entry.setSize64(UInt.MAX_VALUE + 1); // ZIP64!
        assertEquals(UInt.MAX_VALUE + 1, entry.getSize());
        assertEquals(UInt.MAX_VALUE, entry.getSize32());
    }

    @Test
    public void testOffset64() {
        try {
            entry.setOffset64(UNKNOWN - 1);
            fail();
        } catch (IllegalArgumentException expected) {
        }

        try {
            entry.setOffset64(ULong.MIN_VALUE - 1);
            fail();
        } catch (IllegalArgumentException expected) {
        }

        try {
            entry.setOffset64(ULong.MAX_VALUE + 1);
            fail();
        } catch (IllegalArgumentException expected) {
        }

        assertEquals(UNKNOWN, entry.getOffset());
        assertEquals(UNKNOWN, entry.getOffset32());

        entry.setOffset64(UInt.MIN_VALUE);
        assertEquals(UInt.MIN_VALUE, entry.getOffset());
        assertEquals(UInt.MIN_VALUE, entry.getOffset32());
        entry.setOffset64(UInt.MAX_VALUE);
        assertEquals(UInt.MAX_VALUE, entry.getOffset());
        assertEquals(UInt.MAX_VALUE, entry.getOffset32());
        entry.setOffset64(UInt.MAX_VALUE + 1); // ZIP64!
        assertEquals(UInt.MAX_VALUE + 1, entry.getOffset());
        assertEquals(UInt.MAX_VALUE, entry.getOffset32());
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

        entry.setSize64(UInt.MAX_VALUE);
        entry.setCompressedSize64(UInt.MAX_VALUE);
        entry.setOffset64(UInt.MAX_VALUE);
        entry.setExtra(set); // this must be last in the sequence!
        assertEquals(0x0fedcba987654321L, entry.getSize());
        assertEquals(UInt.MAX_VALUE, entry.getSize32());
        assertEquals(0x0fedcba987654322L, entry.getCompressedSize());
        assertEquals(UInt.MAX_VALUE, entry.getCompressedSize32());
        assertEquals(0x0fedcba987654323L, entry.getOffset());
        assertEquals(UInt.MAX_VALUE, entry.getOffset32());

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
