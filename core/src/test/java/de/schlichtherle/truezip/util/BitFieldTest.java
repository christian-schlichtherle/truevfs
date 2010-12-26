/*
 * Copyright 2007-2010 Schlichtherle IT Services
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
package de.schlichtherle.truezip.util;

import org.junit.Test;

import static de.schlichtherle.truezip.util.BitFieldTest.Dummy.*;
import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

/**
 * @author Christian Schlichtherle
 * @version $Id$
 */
public class BitFieldTest {

    @Test
    public void testSetOne() {
        BitField<Dummy> bits = BitField.noneOf(Dummy.class).set(ONE);
        assertFalse(bits.isEmpty());
        assertThat(bits.cardinality(), is(1));
        assertTrue(bits.get(ONE));
        assertTrue(bits.is(ONE));
        assertThat(BitField.of(bits.toEnumSet()), equalTo(bits));
    }

    @Test
    public void testClearOne() {
        BitField<Dummy> bits = BitField.of(ONE).clear(ONE);
        assertTrue(bits.isEmpty());
        assertThat(bits.cardinality(), is(0));
        assertFalse(bits.get(ONE));
        assertFalse(bits.is(ONE));
        assertThat(BitField.of(bits.toEnumSet()), equalTo(bits));
    }

    @Test
    public void testSetTwo() {
        BitField<Dummy> bits = BitField.of(ONE, TWO);
        assertFalse(bits.isEmpty());
        assertThat(bits.cardinality(), is(2));
        assertTrue(bits.get(ONE));
        assertTrue(bits.is(ONE));
        assertTrue(bits.get(TWO));
        assertTrue(bits.is(TWO));
        assertThat(BitField.of(bits.toEnumSet()), equalTo(bits));
    }

    @Test
    public void testClearTwo() {
        BitField<Dummy> bits = BitField.of(ONE, TWO).clear(ONE).clear(TWO);
        assertTrue(bits.isEmpty());
        assertThat(bits.cardinality(), is(0));
        assertFalse(bits.get(ONE));
        assertFalse(bits.is(ONE));
        assertFalse(bits.get(TWO));
        assertFalse(bits.is(TWO));
        assertThat(BitField.of(bits.toEnumSet()), equalTo(bits));
    }

    @Test
    public void testAllOf() {
        BitField<Dummy> bits = BitField.allOf(Dummy.class);
        assertThat(bits.cardinality(), is(3));
    }

    /*@Test
    public void testClear() {
        BitField<Dummy> bits = BitField.allOf(Dummy.class);
        assertThat(bits.cardinality(), is(3));
        bits = bits.clear();
        assertThat(bits.cardinality(), is(0));
        assertSame(bits, bits.clear());
    }*/

    @Test
    public void testNot() {
        BitField<Dummy> bits = BitField.allOf(Dummy.class);
        assertThat(bits.cardinality(), is(3));
        bits = bits.not();
        assertThat(bits.cardinality(), is(0));
    }

    @Test
    public void testAnd() {
        BitField<Dummy> bits = BitField.allOf(Dummy.class);
        assertThat(bits.cardinality(), is(3));
        assertThat(bits.and(BitField.allOf(Dummy.class)), sameInstance(bits));
        bits = bits.and(BitField.noneOf(Dummy.class));
        assertThat(bits.cardinality(), is(0));
    }

    @Test
    public void testOr() {
        BitField<Dummy> bits = BitField.noneOf(Dummy.class);
        assertThat(bits.cardinality(), is(0));
        assertThat(bits.and(BitField.noneOf(Dummy.class)), sameInstance(bits));
        bits = bits.or(BitField.allOf(Dummy.class));
        assertThat(bits.cardinality(), is(3));
    }

    enum Dummy { ONE, TWO, THREE }
}
