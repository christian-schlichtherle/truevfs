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
    public void testEncodedPlatform() {
        try {
            entry.setEncodedPlatform(UNKNOWN - 1);
            fail();
        } catch (IllegalArgumentException expected) {
        }

        try {
            entry.setEncodedPlatform(UByte.MIN_VALUE - 1);
            fail();
        } catch (IllegalArgumentException expected) {
        }

        try {
            entry.setEncodedPlatform(UByte.MAX_VALUE + 1);
            fail();
        } catch (IllegalArgumentException expected) {
        }

        assertEquals(0, entry.getEncodedPlatform());
        entry.setEncodedPlatform(UByte.MIN_VALUE);
        assertEquals(UByte.MIN_VALUE, entry.getEncodedPlatform());
        entry.setEncodedPlatform(PLATFORM_FAT);
        assertEquals(PLATFORM_FAT, entry.getEncodedPlatform());
        entry.setEncodedPlatform(PLATFORM_UNIX);
        assertEquals(PLATFORM_UNIX, entry.getEncodedPlatform());
        entry.setEncodedPlatform(UByte.MAX_VALUE);
        assertEquals(UByte.MAX_VALUE, entry.getEncodedPlatform());
    }

    @Test
    public void testGeneralPurposeBitFlags() {
        try {
            entry.setGeneralPurposeBitFlags(UNKNOWN - 1);
            fail();
        } catch (IllegalArgumentException expected) {
        }

        try {
            entry.setGeneralPurposeBitFlags(UShort.MIN_VALUE - 1);
            fail();
        } catch (IllegalArgumentException expected) {
        }

        try {
            entry.setGeneralPurposeBitFlags(UShort.MAX_VALUE + 1);
            fail();
        } catch (IllegalArgumentException expected) {
        }

        assertEquals(UShort.MIN_VALUE, entry.getGeneralPurposeBitFlags());
        entry.setGeneralPurposeBitFlags(GPBF_ENCRYPTED);
        assertEquals(GPBF_ENCRYPTED, entry.getGeneralPurposeBitFlags());
        entry.setGeneralPurposeBitFlags(GPBF_DATA_DESCRIPTOR);
        assertEquals(GPBF_DATA_DESCRIPTOR, entry.getGeneralPurposeBitFlags());
        entry.setGeneralPurposeBitFlags(GPBF_UTF8);
        assertEquals(GPBF_UTF8, entry.getGeneralPurposeBitFlags());
        entry.setGeneralPurposeBitFlags(UShort.MAX_VALUE);
        assertEquals(UShort.MAX_VALUE, entry.getGeneralPurposeBitFlags());
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
    public void testEncodedMethod() {
        try {
            entry.setEncodedMethod(UNKNOWN - 1);
            fail();
        } catch (IllegalArgumentException expected) {
        }

        try {
            entry.setEncodedMethod(UShort.MIN_VALUE - 1);
            fail();
        } catch (IllegalArgumentException expected) {
        }

        try {
            entry.setEncodedMethod(UShort.MAX_VALUE + 1);
            fail();
        } catch (IllegalArgumentException expected) {
        }

        assertEquals(0, entry.getEncodedMethod());
        entry.setEncodedMethod(UShort.MIN_VALUE);
        assertEquals(UShort.MIN_VALUE, entry.getEncodedMethod());
        entry.setEncodedMethod(STORED);
        assertEquals(STORED, entry.getEncodedMethod());
        entry.setEncodedMethod(DEFLATED);
        assertEquals(DEFLATED, entry.getEncodedMethod());
        entry.setEncodedMethod(UShort.MAX_VALUE);
        assertEquals(UShort.MAX_VALUE, entry.getEncodedMethod());
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
    public void testEncodedTime() {
        try {
            entry.setEncodedTime(UNKNOWN - 1);
            fail();
        } catch (IllegalArgumentException expected) {
        }

        try {
            entry.setEncodedTime(UInt.MIN_VALUE - 1);
            fail();
        } catch (IllegalArgumentException expected) {
        }

        try {
            entry.setEncodedTime(UInt.MAX_VALUE + 1);
            fail();
        } catch (IllegalArgumentException expected) {
        }

        assertEquals(0, entry.getEncodedTime());

        entry.setEncodedTime(UInt.MIN_VALUE);
        assertEquals(UInt.MIN_VALUE, entry.getEncodedTime());
        entry.setEncodedTime(UInt.MAX_VALUE);
        assertEquals(UInt.MAX_VALUE, entry.getEncodedTime());
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
    public void testEncodedCrc() {
        try {
            entry.setEncodedCrc(UNKNOWN - 1);
            fail();
        } catch (IllegalArgumentException expected) {
        }

        try {
            entry.setEncodedCrc(UInt.MIN_VALUE - 1);
            fail();
        } catch (IllegalArgumentException expected) {
        }

        try {
            entry.setEncodedCrc(UInt.MAX_VALUE + 1);
            fail();
        } catch (IllegalArgumentException expected) {
        }

        assertEquals(0, entry.getEncodedCrc());
        entry.setEncodedCrc(UInt.MIN_VALUE);
        assertEquals(UInt.MIN_VALUE, entry.getEncodedCrc());
        entry.setEncodedCrc(UInt.MAX_VALUE);
        assertEquals(UInt.MAX_VALUE, entry.getEncodedCrc());
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

        entry.setCompressedSize(UInt.MIN_VALUE);
        assertEquals(UInt.MIN_VALUE, entry.getCompressedSize());
        entry.setCompressedSize(UInt.MAX_VALUE);
        assertEquals(UInt.MAX_VALUE, entry.getCompressedSize());
        entry.setCompressedSize(UInt.MAX_VALUE + 1); // ZIP64!
        assertEquals(UInt.MAX_VALUE + 1, entry.getCompressedSize());
        entry.setCompressedSize(UNKNOWN);
        assertEquals(UNKNOWN, entry.getCompressedSize());
    }

    @Test
    public void testEncodedCompressedSize() {
        try {
            entry.setEncodedCompressedSize(UNKNOWN - 1);
            fail();
        } catch (IllegalArgumentException expected) {
        }

        try {
            entry.setEncodedCompressedSize(ULong.MIN_VALUE - 1);
            fail();
        } catch (IllegalArgumentException expected) {
        }

        try {
            entry.setEncodedCompressedSize(ULong.MAX_VALUE + 1);
            fail();
        } catch (IllegalArgumentException expected) {
        }

        assertEquals(0, entry.getEncodedCompressedSize());

        entry.setEncodedCompressedSize(UInt.MIN_VALUE);
        assertEquals(UInt.MIN_VALUE, entry.getEncodedCompressedSize());
        entry.setEncodedCompressedSize(UInt.MAX_VALUE);
        assertEquals(UInt.MAX_VALUE, entry.getEncodedCompressedSize());
        entry.setEncodedCompressedSize(UInt.MAX_VALUE + 1); // ZIP64!
        assertEquals(UInt.MAX_VALUE, entry.getEncodedCompressedSize());
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

        entry.setSize(UInt.MIN_VALUE);
        assertEquals(UInt.MIN_VALUE, entry.getSize());
        entry.setSize(UInt.MAX_VALUE);
        assertEquals(UInt.MAX_VALUE, entry.getSize());
        entry.setSize(UInt.MAX_VALUE + 1); // ZIP64!
        assertEquals(UInt.MAX_VALUE + 1, entry.getSize());
        entry.setSize(UNKNOWN);
        assertEquals(UNKNOWN, entry.getSize());
    }

    @Test
    public void testEncodedSize() {
        try {
            entry.setEncodedSize(UNKNOWN - 1);
            fail();
        } catch (IllegalArgumentException expected) {
        }

        try {
            entry.setEncodedSize(ULong.MIN_VALUE - 1);
            fail();
        } catch (IllegalArgumentException expected) {
        }

        try {
            entry.setEncodedSize(ULong.MAX_VALUE + 1);
            fail();
        } catch (IllegalArgumentException expected) {
        }

        assertEquals(0, entry.getEncodedSize());

        entry.setEncodedSize(UInt.MIN_VALUE);
        assertEquals(UInt.MIN_VALUE, entry.getEncodedSize());
        entry.setEncodedSize(UInt.MAX_VALUE);
        assertEquals(UInt.MAX_VALUE, entry.getEncodedSize());
        entry.setEncodedSize(UInt.MAX_VALUE + 1); // ZIP64!
        assertEquals(UInt.MAX_VALUE, entry.getEncodedSize());
    }

    @Test
    public void testEncodedOffset() {
        try {
            entry.setEncodedOffset(UNKNOWN - 1);
            fail();
        } catch (IllegalArgumentException expected) {
        }

        try {
            entry.setEncodedOffset(ULong.MIN_VALUE - 1);
            fail();
        } catch (IllegalArgumentException expected) {
        }

        try {
            entry.setEncodedOffset(ULong.MAX_VALUE + 1);
            fail();
        } catch (IllegalArgumentException expected) {
        }

        assertEquals(UNKNOWN, entry.getOffset());
        assertEquals(0, entry.getEncodedOffset());

        entry.setEncodedOffset(UInt.MIN_VALUE);
        assertEquals(UInt.MIN_VALUE, entry.getOffset());
        assertEquals(UInt.MIN_VALUE, entry.getEncodedOffset());
        entry.setEncodedOffset(UInt.MAX_VALUE);
        assertEquals(UInt.MAX_VALUE, entry.getOffset());
        assertEquals(UInt.MAX_VALUE, entry.getEncodedOffset());
        entry.setEncodedOffset(UInt.MAX_VALUE + 1); // ZIP64!
        assertEquals(UInt.MAX_VALUE + 1, entry.getOffset());
        assertEquals(UInt.MAX_VALUE, entry.getEncodedOffset());
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

        entry.setEncodedSize(UInt.MAX_VALUE);
        entry.setEncodedCompressedSize(UInt.MAX_VALUE);
        entry.setEncodedOffset(UInt.MAX_VALUE);
        entry.setEncodedExtraFields(set); // this must be last in the sequence!
        assertEquals(0x0fedcba987654321L, entry.getSize());
        assertEquals(UInt.MAX_VALUE, entry.getEncodedSize());
        assertEquals(0x0fedcba987654322L, entry.getCompressedSize());
        assertEquals(UInt.MAX_VALUE, entry.getEncodedCompressedSize());
        assertEquals(0x0fedcba987654323L, entry.getOffset());
        assertEquals(UInt.MAX_VALUE, entry.getEncodedOffset());

        set[0] = (byte) 0xff;

        byte[] got1 = entry.getEncodedExtraFields();
        assertNotNull(got1);
        assertNotSame(set, got1);

        final byte[] got2 = entry.getEncodedExtraFields();
        assertNotNull(got2);
        assertNotSame(set, got2);

        assertNotSame(got1, got2);

        set[0] = (byte) 0x00;

        assertTrue(Arrays.equals(set, got1));
        assertTrue(Arrays.equals(set, got2));
    }
}
