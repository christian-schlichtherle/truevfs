/*
 * Copyright © 2005 - 2021 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.comp.zip;

import java.util.Arrays;
import static org.junit.Assert.*;
import org.junit.Test;

/**
 * A test case for the {@link DefaultExtraField} class.
 *
 * @author Christian Schlichtherle
 */
public final class DefaultExtraFieldTest {

    private DefaultExtraField field = new DefaultExtraField(0x0000);

    @Test
    public void testConstructor() {
        try {
            field = new DefaultExtraField(UShort.MIN_VALUE - 1);
            fail();
        } catch (IllegalArgumentException expected) {
        }

        try {
            field = new DefaultExtraField(UShort.MAX_VALUE + 1);
            fail();
        } catch (IllegalArgumentException expected) {
        }

        field = new DefaultExtraField(UShort.MIN_VALUE);
        assertEquals(UShort.MIN_VALUE, field.getHeaderId());
        assertEquals(0, field.getDataSize());

        field = new DefaultExtraField(UShort.MAX_VALUE);
        assertEquals(UShort.MAX_VALUE, field.getHeaderId());
        assertEquals(0, field.getDataSize());
    }

    @Test
    public void testGetDataSize() {
        assertEquals(0, field.getDataSize());
    }

    @Test
    public void testGetDataBlock() {
        assertEquals(0, field.getDataBlock().length);
    }

    @Test
    public void testReadWrite() {
        final byte[] read = new byte[11];

        try {
            field.readFrom(read, 1, UShort.MIN_VALUE - 1);
            fail();
        } catch (IllegalArgumentException expected) {
        }
        try {
            field.readFrom(read, 1, read.length);
            fail();
        } catch (IndexOutOfBoundsException expected) {
        }
        try {
            field.readFrom(read, read.length, 1);
            fail();
        } catch (IndexOutOfBoundsException expected) {
        }
        try {
            field.readFrom(read, 1, UShort.MAX_VALUE + 1);
            fail();
        } catch (IllegalArgumentException expected) {
        }
        field.readFrom(read, 1, read.length - 1);
        assertEquals(read.length - 1, field.getDataSize());

        read[1] = (byte) 0xff;

        final byte[] write = new byte[11];
        field.writeTo(write, 1);

        read[1] = (byte) 0x00;

        assertTrue(Arrays.equals(read, write));
    }
}