/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package de.schlichtherle.truezip.zip;

import static de.schlichtherle.truezip.zip.ZipEntry.*;
import java.util.Arrays;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;

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
    public void testRawPlatform() {
        try {
            entry.setRawPlatform(UNKNOWN - 1);
            fail();
        } catch (IllegalArgumentException expected) {
        }

        try {
            entry.setRawPlatform(UByte.MIN_VALUE - 1);
            fail();
        } catch (IllegalArgumentException expected) {
        }

        try {
            entry.setRawPlatform(UByte.MAX_VALUE + 1);
            fail();
        } catch (IllegalArgumentException expected) {
        }

        assertEquals(0, entry.getRawPlatform());
        entry.setRawPlatform(UByte.MIN_VALUE);
        assertEquals(UByte.MIN_VALUE, entry.getRawPlatform());
        entry.setRawPlatform(PLATFORM_FAT);
        assertEquals(PLATFORM_FAT, entry.getRawPlatform());
        entry.setRawPlatform(PLATFORM_UNIX);
        assertEquals(PLATFORM_UNIX, entry.getRawPlatform());
        entry.setRawPlatform(UByte.MAX_VALUE);
        assertEquals(UByte.MAX_VALUE, entry.getRawPlatform());
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
        entry.setMethod(BZIP2);
        assertEquals(BZIP2, entry.getMethod());
        entry.setMethod(UNKNOWN);
        assertEquals(UNKNOWN, entry.getMethod());
    }

    @Test
    public void testRawMethod() {
        try {
            entry.setRawMethod(UNKNOWN - 1);
            fail();
        } catch (IllegalArgumentException expected) {
        }

        try {
            entry.setRawMethod(UShort.MIN_VALUE - 1);
            fail();
        } catch (IllegalArgumentException expected) {
        }

        try {
            entry.setRawMethod(UShort.MAX_VALUE + 1);
            fail();
        } catch (IllegalArgumentException expected) {
        }

        assertEquals(0, entry.getRawMethod());
        entry.setRawMethod(UShort.MIN_VALUE);
        assertEquals(UShort.MIN_VALUE, entry.getRawMethod());
        entry.setRawMethod(STORED);
        assertEquals(STORED, entry.getRawMethod());
        entry.setRawMethod(DEFLATED);
        assertEquals(DEFLATED, entry.getRawMethod());
        entry.setRawMethod(UShort.MAX_VALUE);
        assertEquals(UShort.MAX_VALUE, entry.getRawMethod());
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
    public void testRawTime() {
        try {
            entry.setRawTime(UNKNOWN - 1);
            fail();
        } catch (IllegalArgumentException expected) {
        }

        try {
            entry.setRawTime(UInt.MIN_VALUE - 1);
            fail();
        } catch (IllegalArgumentException expected) {
        }

        try {
            entry.setRawTime(UInt.MAX_VALUE + 1);
            fail();
        } catch (IllegalArgumentException expected) {
        }

        assertEquals(0, entry.getRawTime());

        entry.setRawTime(UInt.MIN_VALUE);
        assertEquals(UInt.MIN_VALUE, entry.getRawTime());
        entry.setRawTime(UInt.MAX_VALUE);
        assertEquals(UInt.MAX_VALUE, entry.getRawTime());
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
    public void testRawCrc() {
        try {
            entry.setRawCrc(UNKNOWN - 1);
            fail();
        } catch (IllegalArgumentException expected) {
        }

        try {
            entry.setRawCrc(UInt.MIN_VALUE - 1);
            fail();
        } catch (IllegalArgumentException expected) {
        }

        try {
            entry.setRawCrc(UInt.MAX_VALUE + 1);
            fail();
        } catch (IllegalArgumentException expected) {
        }

        assertEquals(0, entry.getRawCrc());
        entry.setRawCrc(UInt.MIN_VALUE);
        assertEquals(UInt.MIN_VALUE, entry.getRawCrc());
        entry.setRawCrc(UInt.MAX_VALUE);
        assertEquals(UInt.MAX_VALUE, entry.getRawCrc());
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
    public void testRawCompressedSize() {
        try {
            entry.setRawCompressedSize(UNKNOWN - 1);
            fail();
        } catch (IllegalArgumentException expected) {
        }

        try {
            entry.setRawCompressedSize(ULong.MIN_VALUE - 1);
            fail();
        } catch (IllegalArgumentException expected) {
        }

        try {
            entry.setRawCompressedSize(ULong.MAX_VALUE + 1);
            fail();
        } catch (IllegalArgumentException expected) {
        }

        assertEquals(0, entry.getRawCompressedSize());

        entry.setRawCompressedSize(UInt.MIN_VALUE);
        assertEquals(UInt.MIN_VALUE, entry.getRawCompressedSize());
        entry.setRawCompressedSize(UInt.MAX_VALUE);
        assertEquals(UInt.MAX_VALUE, entry.getRawCompressedSize());
        entry.setRawCompressedSize(UInt.MAX_VALUE + 1); // ZIP64!
        assertEquals(UInt.MAX_VALUE, entry.getRawCompressedSize());
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
    public void testRawSize() {
        try {
            entry.setRawSize(UNKNOWN - 1);
            fail();
        } catch (IllegalArgumentException expected) {
        }

        try {
            entry.setRawSize(ULong.MIN_VALUE - 1);
            fail();
        } catch (IllegalArgumentException expected) {
        }

        try {
            entry.setRawSize(ULong.MAX_VALUE + 1);
            fail();
        } catch (IllegalArgumentException expected) {
        }

        assertEquals(0, entry.getRawSize());

        entry.setRawSize(UInt.MIN_VALUE);
        assertEquals(UInt.MIN_VALUE, entry.getRawSize());
        entry.setRawSize(UInt.MAX_VALUE);
        assertEquals(UInt.MAX_VALUE, entry.getRawSize());
        entry.setRawSize(UInt.MAX_VALUE + 1); // ZIP64!
        assertEquals(UInt.MAX_VALUE, entry.getRawSize());
    }

    @Test
    public void testExternalAttributes() {
        try {
            entry.setExternalAttributes(UNKNOWN - 1);
            fail();
        } catch (IllegalArgumentException expected) {
        }

        try {
            entry.setExternalAttributes(UInt.MAX_VALUE + 1);
            fail();
        } catch (IllegalArgumentException expected) {
        }

        assertEquals(UNKNOWN, entry.getExternalAttributes());
        entry.setExternalAttributes(UInt.MIN_VALUE);
        assertEquals(UInt.MIN_VALUE, entry.getExternalAttributes());
        entry.setExternalAttributes(UInt.MAX_VALUE);
        assertEquals(UInt.MAX_VALUE, entry.getExternalAttributes());
        entry.setExternalAttributes(UNKNOWN);
        assertEquals(UNKNOWN, entry.getExternalAttributes());
    }

    @Test
    public void testRawExternalAttributes() {
        try {
            entry.setRawExternalAttributes(UNKNOWN - 1);
            fail();
        } catch (IllegalArgumentException expected) {
        }

        try {
            entry.setRawExternalAttributes(UInt.MIN_VALUE - 1);
            fail();
        } catch (IllegalArgumentException expected) {
        }

        try {
            entry.setRawExternalAttributes(UInt.MAX_VALUE + 1);
            fail();
        } catch (IllegalArgumentException expected) {
        }

        assertEquals(0, entry.getRawExternalAttributes());
        entry.setRawExternalAttributes(UInt.MIN_VALUE);
        assertEquals(UInt.MIN_VALUE, entry.getRawExternalAttributes());
        entry.setRawExternalAttributes(UInt.MAX_VALUE);
        assertEquals(UInt.MAX_VALUE, entry.getRawExternalAttributes());
    }

    @Test
    public void testRawOffset() {
        try {
            entry.setRawOffset(UNKNOWN - 1);
            fail();
        } catch (IllegalArgumentException expected) {
        }

        try {
            entry.setRawOffset(ULong.MIN_VALUE - 1);
            fail();
        } catch (IllegalArgumentException expected) {
        }

        try {
            entry.setRawOffset(ULong.MAX_VALUE + 1);
            fail();
        } catch (IllegalArgumentException expected) {
        }

        assertEquals(UNKNOWN, entry.getOffset());
        assertEquals(0, entry.getRawOffset());

        entry.setRawOffset(UInt.MIN_VALUE);
        assertEquals(UInt.MIN_VALUE, entry.getOffset());
        assertEquals(UInt.MIN_VALUE, entry.getRawOffset());
        entry.setRawOffset(UInt.MAX_VALUE);
        assertEquals(UInt.MAX_VALUE, entry.getOffset());
        assertEquals(UInt.MAX_VALUE, entry.getRawOffset());
        entry.setRawOffset(UInt.MAX_VALUE + 1); // ZIP64!
        assertEquals(UInt.MAX_VALUE + 1, entry.getOffset());
        assertEquals(UInt.MAX_VALUE, entry.getRawOffset());
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

        entry.setRawSize(UInt.MAX_VALUE);
        entry.setRawCompressedSize(UInt.MAX_VALUE);
        entry.setRawOffset(UInt.MAX_VALUE);
        entry.setRawExtraFields(set); // this must be last in the sequence!
        assertEquals(0x0fedcba987654321L, entry.getSize());
        assertEquals(UInt.MAX_VALUE, entry.getRawSize());
        assertEquals(0x0fedcba987654322L, entry.getCompressedSize());
        assertEquals(UInt.MAX_VALUE, entry.getRawCompressedSize());
        assertEquals(0x0fedcba987654323L, entry.getOffset());
        assertEquals(UInt.MAX_VALUE, entry.getRawOffset());

        set[0] = (byte) 0xff;

        byte[] got1 = entry.getRawExtraFields();
        assertNotNull(got1);
        assertNotSame(set, got1);

        final byte[] got2 = entry.getRawExtraFields();
        assertNotNull(got2);
        assertNotSame(set, got2);

        assertNotSame(got1, got2);

        set[0] = (byte) 0x00;

        assertTrue(Arrays.equals(set, got1));
        assertTrue(Arrays.equals(set, got2));
    }
}
