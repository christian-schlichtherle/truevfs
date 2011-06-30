/*
 * Copyright (C) 2007-2011 Schlichtherle IT Services
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

import static org.junit.Assert.*;

/**
 * A simple round trip test of the little endian utility methods.
 * 
 * @author Christian Schlichtherle
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
    public void testShort() {
        final short s = LittleEndian.readShort(read, 1);
        assertEquals((short) 0xaa55, s);
        LittleEndian.writeShort(s, write, 1);
        LittleEndian.writeShort(s, write, 3);
        LittleEndian.writeShort(s, write, 5);
        LittleEndian.writeShort(s, write, 7);
        assertTrue(Arrays.equals(read, write));
    }

    @Test
    public void testUShort() {
        final int s = LittleEndian.readUShort(read, 1);
        assertEquals(0xaa55, s);
        LittleEndian.writeShort((short) s, write, 1);
        LittleEndian.writeShort((short) s, write, 3);
        LittleEndian.writeShort((short) s, write, 5);
        LittleEndian.writeShort((short) s, write, 7);
        assertTrue(Arrays.equals(read, write));
    }

    @Test
    public void testInt() {
        final int i = LittleEndian.readInt(read, 1);
        assertEquals(0xaa55aa55, i);
        LittleEndian.writeInt(i, write, 1);
        LittleEndian.writeInt(i, write, 5);
        assertTrue(Arrays.equals(read, write));
    }

    @Test
    public void testUInt() {
        final long i = LittleEndian.readUInt(read, 1);
        assertEquals(0xaa55aa55L, i);
        LittleEndian.writeInt((int) i, write, 1);
        LittleEndian.writeInt((int) i, write, 5);
        assertTrue(Arrays.equals(read, write));
    }

    @Test
    public void testLong() {
        final long l = LittleEndian.readLong(read, 1);
        assertEquals(0xaa55aa55aa55aa55L, l);
        LittleEndian.writeLong(l, write, 1);
        assertTrue(Arrays.equals(read, write));
    }
}
