/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.comp.zip;

import java.util.zip.ZipException;
import net.java.truecommons.io.MutableBuffer;
import static org.junit.Assert.*;
import org.junit.Test;

/**
 * Tests the collection of {@link ExtraFields Extra Fields}.
 *
 * @author Christian Schlichtherle
 */
public final class ExtraFieldsTest {

    // Serialized Extra Fields in little endian order.
    private final byte[] serialized = new byte[] {
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
    private final ExtraFields fields = new ExtraFields();

    @Test
    public void testCollection1() throws ZipException {
        fields.parse(MutableBuffer
                .wrap(serialized)
                .littleEndian()
                .asImmutableBuffer());
        final ExtraField ef = fields.get(ExtraFields.ZIP64_HEADER_ID);
        assertNotNull(ef);
        assertSame(ef, fields.remove(ExtraFields.ZIP64_HEADER_ID));
        assertNull(fields.get(ExtraFields.ZIP64_HEADER_ID));
        assertNull(fields.add(ef));
    }

    @Test
    public void testCollection2() throws ZipException {
        assertEquals(0, fields.getTotalSize());
        final ExtraField
                ef = new BufferedExtraField(ExtraFields.ZIP64_HEADER_ID, 0);
        assertNull(fields.get(ExtraFields.ZIP64_HEADER_ID));
        assertNull(fields.add(ef));
        assertEquals(ef.getTotalSize(), fields.getTotalSize());
        assertSame(ef, fields.remove(ExtraFields.ZIP64_HEADER_ID));
        assertEquals(0, fields.getTotalSize());
    }
}
