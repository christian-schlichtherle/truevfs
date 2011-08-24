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

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * A test case for the {@link ExtraField} class.
 * 
 * @author Christian Schlichtherle
 * @version $Id$
 */
public final class ExtraFieldTest {

    @Test
    public void testRegister() {
        try {
            ExtraField.register(null);
            fail();
        } catch (NullPointerException expected) {
        }

        try {
            ExtraField.register(TooSmallHeaderIDExtraField.class);
            fail();
        } catch (IllegalArgumentException expected) {
        }

        try {
            ExtraField.register(TooLargeHeaderIDExtraField.class);
            fail();
        } catch (IllegalArgumentException expected) {
        }
        
        ExtraField.register(NullExtraField.class);
    }

    @Test
    public void testCreate() {
        ExtraField ef;

        ExtraField.register(NullExtraField.class);
        ef = ExtraField.create(0x0000);
        assertTrue(ef instanceof NullExtraField);
        assertEquals(0x0000, ef.getHeaderId());

        ef = ExtraField.create(0x0001);
        //assertTrue(ef instanceof Zip64ExtraField);
        assertTrue(ef instanceof DefaultExtraField);
        assertEquals(0x0001, ef.getHeaderId());

        ef = ExtraField.create(0x0002);
        assertTrue(ef instanceof DefaultExtraField);
        assertEquals(0x0002, ef.getHeaderId());

        ef = ExtraField.create(UShort.MAX_VALUE);
        assertTrue(ef instanceof DefaultExtraField);
        assertEquals(UShort.MAX_VALUE, ef.getHeaderId());

        try {
            ef = ExtraField.create(UShort.MIN_VALUE - 1);
            fail();
        } catch (IllegalArgumentException expected) {
        }

        try {
            ef = ExtraField.create(UShort.MAX_VALUE + 1);
            fail();
        } catch (IllegalArgumentException expected) {
        }
    }

    private static class NullExtraField extends ExtraField {
        public NullExtraField() {
        }

        @Override
        public int getHeaderId() {
            return 0x0000;
        }

        @Override
        int getDataSize() {
            return 0;
        }

        @Override
        void readFrom(byte[] data, int off, int size) {
        }

        @Override
        void writeTo(byte[] data, int off) {
        }
    }

    private static class TooSmallHeaderIDExtraField extends NullExtraField {
        @Override
        public int getHeaderId() {
            return UShort.MIN_VALUE - 1; // illegal return value
        }
    }

    private static class TooLargeHeaderIDExtraField extends NullExtraField {
        @Override
        public int getHeaderId() {
            return UShort.MAX_VALUE + 1; // illegal return value
        }
    }
}
