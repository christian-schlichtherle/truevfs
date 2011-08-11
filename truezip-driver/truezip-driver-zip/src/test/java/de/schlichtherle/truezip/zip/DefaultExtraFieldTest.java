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

import java.util.*;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * A test case for the {@link DefaultExtraField} class.
 * 
 * @author Christian Schlichtherle
 * @version $Id$
 */
public final class DefaultExtraFieldTest {

    private DefaultExtraField def;

    @Before
    public void setUp() {
        def = new DefaultExtraField(0x0000);
    }

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
