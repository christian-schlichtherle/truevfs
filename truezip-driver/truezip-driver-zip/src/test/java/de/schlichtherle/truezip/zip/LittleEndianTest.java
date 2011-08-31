/*
 * Copyright (C) 2007-2011 Schlichtherle IT Services
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package de.schlichtherle.truezip.zip;

import static de.schlichtherle.truezip.zip.LittleEndian.*;
import java.util.Arrays;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;

/**
 * A simple round trip test of the little endian utility methods.
 * 
 * @author  Christian Schlichtherle
 * @version $Id$
 */
public final class LittleEndianTest {

    private static final byte[] DATA = {
        (byte) 0x00, // use an offset of one for testing
        (byte) 0x55, (byte) 0xaa, (byte) 0x55, (byte) 0xaa,
        (byte) 0x55, (byte) 0xaa, (byte) 0x55, (byte) 0xaa,
    };

    private byte[] read, write;

    @Before
    public void setUp() {
        read = DATA.clone();
        write = new byte[read.length];
    }

    @Test
    public void testByte() {
        final int b1 = readByte(read, 1);
        final int b2 = readByte(read, 2);
        assertEquals((byte) 0x55, b1);
        assertEquals((byte) 0xaa, b2);
        writeByte(b1, write, 1);
        writeByte(b2, write, 2);
        writeByte(b1, write, 3);
        writeByte(b2, write, 4);
        writeByte(b1, write, 5);
        writeByte(b2, write, 6);
        writeByte(b1, write, 7);
        writeByte(b2, write, 8);
        assertTrue(Arrays.equals(read, write));
    }

    @Test
    public void testUByte() {
        final int b1 = readUByte(read, 1);
        final int b2 = readUByte(read, 2);
        assertEquals(0x55, b1);
        assertEquals(0xaa, b2);
        writeByte(b1, write, 1);
        writeByte(b2, write, 2);
        writeByte(b1, write, 3);
        writeByte(b2, write, 4);
        writeByte(b1, write, 5);
        writeByte(b2, write, 6);
        writeByte(b1, write, 7);
        writeByte(b2, write, 8);
        assertTrue(Arrays.equals(read, write));
    }

    @Test
    public void testShort() {
        final int s = readShort(read, 1);
        assertEquals((short) 0xaa55, s);
        writeShort(s, write, 1);
        writeShort(s, write, 3);
        writeShort(s, write, 5);
        writeShort(s, write, 7);
        assertTrue(Arrays.equals(read, write));
    }

    @Test
    public void testUShort() {
        final int s = readUShort(read, 1);
        assertEquals(0xaa55, s);
        writeShort(s, write, 1);
        writeShort(s, write, 3);
        writeShort(s, write, 5);
        writeShort(s, write, 7);
        assertTrue(Arrays.equals(read, write));
    }

    @Test
    public void testInt() {
        final int i = readInt(read, 1);
        assertEquals(0xaa55aa55, i);
        writeInt(i, write, 1);
        writeInt(i, write, 5);
        assertTrue(Arrays.equals(read, write));
    }

    @Test
    public void testUInt() {
        final long i = readUInt(read, 1);
        assertEquals(0xaa55aa55L, i);
        writeInt((int) i, write, 1);
        writeInt((int) i, write, 5);
        assertTrue(Arrays.equals(read, write));
    }

    @Test
    public void testLong() {
        final long l = readLong(read, 1);
        assertEquals(0xaa55aa55aa55aa55L, l);
        writeLong(l, write, 1);
        assertTrue(Arrays.equals(read, write));
    }
}
