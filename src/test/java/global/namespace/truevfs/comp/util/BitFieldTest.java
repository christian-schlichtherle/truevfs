/*
 * Copyright (C) 2005-2015 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package global.namespace.truevfs.comp.util;

import org.junit.Test;

import java.util.Iterator;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.*;

/**
 * @author Christian Schlichtherle
 */
public class BitFieldTest {

    @Test
    public void testSetOne() {
        BitField<Dummy> bits = BitField.noneOf(Dummy.class).set(Dummy.ONE);
        assertFalse(bits.isEmpty());
        assertThat(bits.cardinality(), is(1));
        assertTrue(bits.get(Dummy.ONE));
        assertTrue(bits.is(Dummy.ONE));
        assertThat(BitField.copyOf(bits.toEnumSet()), equalTo(bits));
    }

    @Test
    public void testClearOne() {
        BitField<Dummy> bits = BitField.of(Dummy.ONE).clear(Dummy.ONE);
        assertTrue(bits.isEmpty());
        assertThat(bits.cardinality(), is(0));
        assertFalse(bits.get(Dummy.ONE));
        assertFalse(bits.is(Dummy.ONE));
        assertThat(BitField.copyOf(bits.toEnumSet()), equalTo(bits));
    }

    @Test
    public void testSetTwo() {
        BitField<Dummy> bits = BitField.of(Dummy.ONE, Dummy.TWO);
        assertFalse(bits.isEmpty());
        assertThat(bits.cardinality(), is(2));
        assertTrue(bits.get(Dummy.ONE));
        assertTrue(bits.is(Dummy.ONE));
        assertTrue(bits.get(Dummy.TWO));
        assertTrue(bits.is(Dummy.TWO));
        assertThat(BitField.copyOf(bits.toEnumSet()), equalTo(bits));
    }

    @Test
    public void testClearTwo() {
        BitField<Dummy> bits = BitField.of(Dummy.ONE, Dummy.TWO).clear(Dummy.ONE).clear(Dummy.TWO);
        assertTrue(bits.isEmpty());
        assertThat(bits.cardinality(), is(0));
        assertFalse(bits.get(Dummy.ONE));
        assertFalse(bits.is(Dummy.ONE));
        assertFalse(bits.get(Dummy.TWO));
        assertFalse(bits.is(Dummy.TWO));
        assertThat(BitField.copyOf(bits.toEnumSet()), equalTo(bits));
    }

    @Test
    public void testAllOf() {
        BitField<Dummy> bits = BitField.allOf(Dummy.class);
        assertThat(bits.cardinality(), is(3));
    }

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

    @Test
    public void testIterator() {
        final BitField<Dummy> bits = BitField.allOf(Dummy.class);
        final Iterator<Dummy> it = bits.iterator();
        final Dummy[] dummies = Dummy.values();
        for (int i = 0; i < dummies.length; i++) {
            assert it.hasNext();
            assertThat(it.next(), sameInstance(dummies[i]));
            try {
                it.remove();
                fail();
            } catch (UnsupportedOperationException ex) {
            }
        }
    }

    private enum Dummy {ONE, TWO, THREE}
}
