/*
 * Copyright © 2005 - 2021 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.comp.zip;

import java.util.Arrays;
import static net.java.truevfs.comp.zip.LittleEndian.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

/**
 * A simple round trip test of the little endian utility methods.
 * 
 * @author  Christian Schlichtherle
 */
public final class LittleEndianTest {
    private final byte[] read = {
        (byte) 0x00, // use an offset of one for testing
        (byte) 0x55, (byte) 0xaa, (byte) 0x55, (byte) 0xaa,
        (byte) 0x55, (byte) 0xaa, (byte) 0x55, (byte) 0xaa,
    }, write = new byte[read.length];

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