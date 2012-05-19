/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.truevfs.driver.zip.io;

import net.truevfs.driver.zip.io.DefaultExtraField;
import net.truevfs.driver.zip.io.ExtraField;
import net.truevfs.driver.zip.io.ExtraFields;
import java.util.Arrays;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Tests the collection of {@link ExtraFields Extra Fields}.
 * 
 * @author Christian Schlichtherle
 */
public final class ExtraFieldsTest {

    // Serialized Extra Fields in little endian order.
    private final byte[] SERIALIZED = new byte[] {
        (byte) 0x00, (byte) 0x00, // Header ID: 0x0000 (undefined)
        (byte) 0x00, (byte) 0x00, // Data Size: 0x0000
        (byte) 0x01, (byte) 0x00, // Header ID: 0x0001 (Zip64)
        (byte) 0x1c, (byte) 0x00, // Data Size: 0x001c
        (byte) 0x21, (byte) 0x43, (byte) 0x65, (byte) 0x87, // Original Size
        (byte) 0xa9, (byte) 0xcb, (byte) 0xed, (byte) 0x0f,
        (byte) 0x21, (byte) 0x43, (byte) 0x65, (byte) 0x87, // Compressed Size
        (byte) 0xa9, (byte) 0xcb, (byte) 0xed, (byte) 0x0f,
        (byte) 0x21, (byte) 0x43, (byte) 0x65, (byte) 0x87, // Relative Header Offset
        (byte) 0xa9, (byte) 0xcb, (byte) 0xed, (byte) 0x0f,
        (byte) 0x21, (byte) 0x43, (byte) 0x65, (byte) 0x87, // Disk Start Number
        (byte) 0xfe, (byte) 0xca, // Header ID: 0xcafe (JarMarker)
        (byte) 0x00, (byte) 0x00, // Data Size: 0x0000
    };

    private ExtraFields fields;
    private byte[] serialized;

    @Before
    public void setUp() {
        fields = new ExtraFields();
        serialized = SERIALIZED.clone();
    }

    @Test
    public void testGetSet() {
        assertEquals(0, fields.getExtra().length);

        fields.readFrom(serialized, 0, serialized.length);
        assertEquals(serialized.length, fields.getExtraLength());

        serialized[0] = (byte) 0xff;

        byte[] got1 = fields.getExtra();
        assertNotNull(got1);
        assertNotSame(serialized, got1);

        final byte[] got2 = fields.getExtra();
        assertNotNull(got2);
        assertNotSame(serialized, got2);

        assertNotSame(got1, got2);

        serialized[0] = (byte) 0x00;

        assertTrue(Arrays.equals(serialized, got1));
        assertTrue(Arrays.equals(serialized, got2));
    }

    @Test
    public void testCollection0() {
        fields.readFrom(serialized, 0, serialized.length);
        final ExtraField ef = fields.get(ExtraField.ZIP64_HEADER_ID);
        assertNotNull(ef);
        assertSame(ef, fields.remove(ExtraField.ZIP64_HEADER_ID));
        assertNull(fields.get(ExtraField.ZIP64_HEADER_ID));
        assertNull(fields.add(ef));
        final byte[] got = fields.getExtra();
        assertNotSame(serialized, got);
        assertTrue(Arrays.equals(serialized, got));
    }

    @Test
    public void testCollection1() {
        assertEquals(0, fields.getExtra().length);
        final ExtraField ef = new DefaultExtraField(ExtraField.ZIP64_HEADER_ID);
        assertNull(fields.get(ExtraField.ZIP64_HEADER_ID));
        assertNull(fields.add(ef));
        byte[] got = fields.getExtra();
        assertEquals(4 + ef.getDataSize(), got.length);
        assertSame(ef, fields.remove(ExtraField.ZIP64_HEADER_ID));
        assertEquals(0, fields.getExtra().length);
    }
}