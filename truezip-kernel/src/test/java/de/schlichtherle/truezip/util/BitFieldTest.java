/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.util;

import static de.schlichtherle.truezip.util.BitFieldTest.Dummy.*;
import java.beans.ExceptionListener;
import java.beans.XMLDecoder;
import java.beans.XMLEncoder;
import java.io.*;
import java.lang.reflect.UndeclaredThrowableException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;
import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;
import org.junit.Test;

/**
 * @author Christian Schlichtherle
 */
public class BitFieldTest {

    private static final Logger
            logger = Logger.getLogger(BitFieldTest.class.getName());

    @Test
    public void testSetOne() {
        BitField<Dummy> bits = BitField.noneOf(Dummy.class).set(ONE);
        assertFalse(bits.isEmpty());
        assertThat(bits.cardinality(), is(1));
        assertTrue(bits.get(ONE));
        assertTrue(bits.is(ONE));
        assertThat(BitField.copyOf(bits.toEnumSet()), equalTo(bits));
    }

    @Test
    public void testClearOne() {
        BitField<Dummy> bits = BitField.of(ONE).clear(ONE);
        assertTrue(bits.isEmpty());
        assertThat(bits.cardinality(), is(0));
        assertFalse(bits.get(ONE));
        assertFalse(bits.is(ONE));
        assertThat(BitField.copyOf(bits.toEnumSet()), equalTo(bits));
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
        assertThat(BitField.copyOf(bits.toEnumSet()), equalTo(bits));
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

    @Test
    public void testSerialization() throws IOException, ClassNotFoundException {
        final ExceptionListener listener = new ExceptionListener() {
            @Override
            public void exceptionThrown(Exception ex) {
                throw new UndeclaredThrowableException(ex);
            }
        };

        for (final Dummy[] params : new Dummy[][] {
            // { }, // Doesn't work in Sun's JDK 1.6.* - requires JDK 1.7.0-ea
            { ONE, },
            { ONE, TWO, },
            { ONE, TWO, THREE, },
        }) {
            final BitField<Dummy> original
                    = BitField.copyOf(Arrays.asList(params));

            {
                final ByteArrayOutputStream bos = new ByteArrayOutputStream();
                final ObjectOutputStream oos = new ObjectOutputStream(bos);
                oos.writeObject(original);
                oos.close();

                logger.log(Level.FINEST, "Number of serialized bytes: {0}", bos.size());

                final ByteArrayInputStream bis = new ByteArrayInputStream(bos.toByteArray());
                final ObjectInputStream ois = new ObjectInputStream(bis);
                final Object clone = ois.readObject();
                ois.close();

                assertThat(clone, not(sameInstance((Object) original)));
                assertThat(clone, equalTo((Object) original));
            }

            {
                final ByteArrayOutputStream bos = new ByteArrayOutputStream();
                final XMLEncoder enc = new XMLEncoder(bos);
                enc.setExceptionListener(listener);
                enc.writeObject(original);
                enc.close();

                logger.log(Level.FINEST, bos.toString("UTF-8"));

                final ByteArrayInputStream bis = new ByteArrayInputStream(bos.toByteArray());
                final XMLDecoder dec = new XMLDecoder(bis);
                final Object clone = dec.readObject();
                dec.close();

                assertThat(clone, not(sameInstance((Object) original)));
                assertThat(clone, equalTo((Object) original));
            }
        }
    }

    @SuppressWarnings("PackageVisibleInnerClass")
    enum Dummy { ONE, TWO, THREE }
}
