/*
 * Copyright (C) 2005-2015 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truecommons3.shed;

import org.junit.Test;

import java.util.Iterator;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.junit.Assert.*;

/**
 * @author Christian Schlichtherle
 */
public final class SomeOptionTest extends OptionTestSuite {

    @Override
    String string() { return "Hello world!"; }

    @Test
    public void testComposition() {
        class Container {
            Option<String> getMessage() { return Option.some("Hello world!"); }
        }

        String check = null;
        Option<Container> option = Option.some(new Container());
        for (Container c : option)
            for (String s : c.getMessage())
                check = s;
        assertEquals(new Container().getMessage().get(), check);
    }

    @Override
    public void testEquals() {
        final Option<String> o1 = optionalString();
        final Option<String> o2 = Option.apply(string());
        assertEquals(o1, o2);
        assertEquals(o2, o1);
        assertFalse(o1.equals(Option.apply(null)));
        assertFalse(Option.apply(null).equals(o1));
    }

    @Override
    public void testIterator() {
        final Iterator<String> it = optionalString().iterator();
        assertTrue(it.hasNext());
        assertThat(it.next(), is(sameInstance(string())));
        try {
            it.remove();
            fail();
        } catch (final UnsupportedOperationException ex) {
        }
        assertFalse(it.hasNext());
    }

    @Test
    public void testSize() { assertThat(optionalString().size(), is(1)); }

    @Test
    public void testIsEmpty() { assertFalse(optionalString().isEmpty()); }

    @Test
    public void testGet() { assertSame(string(), optionalString().get()); }

    @Test
    public void testGetOrElse() {
        assertSame(string(), optionalString().getOrElse("foo"));
    }

    @Test
    public void testOrNull() {
        assertSame(string(), optionalString().orNull());
    }

    @Test
    public void testOrElse() {
        assertSame("Hello world!", optionalString().orElse(Option.some("foo")).get());
    }
}
