/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.comp.zip;

import net.java.truevfs.comp.zip.DefaultExtraField;
import net.java.truevfs.comp.zip.UShort;
import java.util.Arrays;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;

/**
 * A test case for the {@link DefaultExtraField} class.
 * 
 * @author Christian Schlichtherle
 */
public final class DefaultExtraFieldTest {

    private DefaultExtraField def = new DefaultExtraField(0x0000);

    @Test
    public void testConstructor() {
        try {
            def = new DefaultExtraField(UShort.MIN_VALUE - 1);
            fail();
        } catch (IllegalArgumentException expected) {
        }

        try {
            def = new DefaultExtraField(UShort.MAX_VALUE + 1);
            fail();
        } catch (IllegalArgumentException expected) {
        }

        def = new DefaultExtraField(UShort.MIN_VALUE);
        assertEquals(UShort.MIN_VALUE, def.getHeaderId());
        assertEquals(0, def.getDataSize());

        def = new DefaultExtraField(UShort.MAX_VALUE);
        assertEquals(UShort.MAX_VALUE, def.getHeaderId());
        assertEquals(0, def.getDataSize());
    }

    @Test
    public void testGetDataSize() {
        assertEquals(0, def.getDataSize());
    }

    @Test
    public void testGetDataBlock() {
        assertEquals(0, def.getDataBlock().length);
    }

    @Test
    public void testReadWrite() {
        final byte[] read = new byte[11];
        
        try {
            def.readFrom(read, 1, UShort.MIN_VALUE - 1);
            fail();
        } catch (IllegalArgumentException expected) {
        }
        try {
            def.readFrom(read, 1, read.length);
            fail();
        } catch (IndexOutOfBoundsException expected) {
        }
        try {
            def.readFrom(read, read.length, 1);
            fail();
        } catch (IndexOutOfBoundsException expected) {
        }
        try {
            def.readFrom(read, 1, UShort.MAX_VALUE + 1);
            fail();
        } catch (IllegalArgumentException expected) {
        }
        def.readFrom(read, 1, read.length - 1);
        assertEquals(read.length - 1, def.getDataSize());
        
        read[1] = (byte) 0xff;
        
        final byte[] write = new byte[11];
        def.writeTo(write, 1);

        read[1] = (byte) 0x00;

        assertTrue(Arrays.equals(read, write));
    }
}