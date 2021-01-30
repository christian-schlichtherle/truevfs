/*
 * Copyright (C) 2005-2015 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truecommons3.shed;

import org.junit.Test;

import java.util.Iterator;
import java.util.NoSuchElementException;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;

/**
 * @author Christian Schlichtherle
 */
public final class NoneOptionTest extends OptionTestSuite {

    @Override
    String string() { return null; }

    @Override
    void assertOptionEquals(Option<String> expected, Option<String> actual) {
        assertSame(expected, actual);
    }

    @Override
    @SuppressWarnings("RedundantStringConstructorCall")
    public void testEquals() {
        final Option<String> o1 = optionalString();
        final Option<String> o2 = Option.apply(string());
        assertEquals(o1, o2);
        assertEquals(o2, o1);
        assertFalse(o1.equals(Option.apply("foo")));
        assertFalse(Option.apply("foo").equals(o1));
    }

    @Test
    public void testIterator() {
        final Iterator<String> it = optionalString().iterator();
        assertFalse(it.hasNext());
    }

    @Test
    public void testSize() {
        assertThat(optionalString().size(), is(0));
    }

    @Test
    public void testIsEmpty() {
        assertTrue(optionalString().isEmpty());
    }

    @Test(expected=NoSuchElementException.class)
    public void testGet() {
        optionalString().get();
    }

    @Test
    public void testGetOrElse() {
        assertSame(optionalString().getOrElse("foo"), "foo");
    }

    @Test
    public void testOrNull() {
        assertNull(optionalString().orNull());
    }

    @Test
    public void testOrElse() {
        assertSame("foo", optionalString().orElse(Option.some("foo")).get());
    }
}
