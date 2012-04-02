/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.truezip.kernel.util;

import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests {@link ExtensionSet}.
 *
 * @author Christian Schlichtherle
 */
public final class ExtensionSetTest {

    private ExtensionSet empty;

    /** An array of equal extension sets. */
    private ExtensionSet[] sets;

    @Before
    public void setUp() {
        empty = new ExtensionSet("");

        sets = new ExtensionSet[] {
            new ExtensionSet("extension1|extension2"),
            new ExtensionSet("extension2|extension1"),
            new ExtensionSet(".extension1|.extension2"),
            new ExtensionSet(".extension2|.extension1"),
            new ExtensionSet("EXTENSION1|EXTENSION2"),
            new ExtensionSet("EXTENSION2|EXTENSION1"),
            new ExtensionSet(".EXTENSION1|.EXTENSION2"),
            new ExtensionSet(".EXTENSION2|.EXTENSION1"),
            new ExtensionSet("ExTeNsIoN1|ExTeNsIoN2"),
            new ExtensionSet("ExTeNsIoN2|ExTeNsIoN1"),
            new ExtensionSet(".eXtEnSiOn1|.eXtEnSiOn2"),
            new ExtensionSet(".eXtEnSiOn2|.eXtEnSiOn1"),
            new ExtensionSet("extension1|.extension2|extension2|.extension1"),
            new ExtensionSet("extension2|.extension1|extension1|.extension2"),
            new ExtensionSet("EXTENSION1|.EXTENSION2|EXTENSION2|.EXTENSION1"),
            new ExtensionSet("EXTENSION2|.EXTENSION1|EXTENSION1|.EXTENSION2"),
            new ExtensionSet("ExTeNsIoN1|.eXtEnSiOn2|ExTeNsIoN2|.eXtEnSiOn1"),
            new ExtensionSet("ExTeNsIoN2|.eXtEnSiOn1|ExTeNsIoN1|.eXtEnSiOn2"),
            new ExtensionSet("extension1|.extension1|EXTENSION1|.EXTENSION1|extension2|.extension2|EXTENSION2|.EXTENSION2"),
            new ExtensionSet("extension2|.extension2|EXTENSION2|.EXTENSION2|extension1|.extension1|EXTENSION1|.EXTENSION1"),
            new ExtensionSet("extension1|.extension1|EXTENSION1|.EXTENSION1|ExTeNsIoN1|.eXtEnSiOn1|extension2|.extension2|EXTENSION2|.EXTENSION2|ExTeNsIoN2|.eXtEnSiOn2"),
            new ExtensionSet("extension2|.extension2|EXTENSION2|.EXTENSION2|ExTeNsIoN2|.eXtEnSiOn2|extension1|.extension1|EXTENSION1|.EXTENSION1|ExTeNsIoN1|.eXtEnSiOn1"),
            new ExtensionSet("extension1||.extension1||EXTENSION1||.EXTENSION1||ExTeNsIoN1||.eXtEnSiOn1||extension2||.extension2||EXTENSION2||.EXTENSION2||ExTeNsIoN2||.eXtEnSiOn2"),
            new ExtensionSet("extension2||.extension2||EXTENSION2||.EXTENSION2||ExTeNsIoN2||.eXtEnSiOn2||extension1||.extension1||EXTENSION1||.EXTENSION1||ExTeNsIoN1||.eXtEnSiOn1"),
        };
    }

    @Test
    public void testEqualsAndHashCode() {
        for (int i = 0; i < sets.length; i++) {
            for (int j = 0; j < sets.length ; j++) {
                assertTrue(sets[i].equals(sets[j]));
                assertEquals(sets[i].hashCode(), sets[j].hashCode());
            }
        }
    }

    @Test
    public void testIsEmpty() {
        assertTrue(empty.isEmpty());

        for (final ExtensionSet set : sets)
            assertFalse(set.isEmpty());
    }

    @Test
    public void testIteratorAndContains() {
        assertFalse(empty.iterator().hasNext());

        for (int i = 0; i < sets.length; i++) {
            for (int j = 0; j < sets.length ; j++) {
                for (final String s : sets[i])
                    assertTrue(sets[j].contains(s));
            }
        }
    }

    @Test
    public void testAddAll() {
        for (int i = 0; i < sets.length; i++)
            for (int j = 0; j < sets.length ; j++)
                assertFalse(sets[i].addAll(sets[j]));
    }

    @Test
    public void testRetainAll() {
        for (int i = 0; i < sets.length; i++)
            for (int j = 0; j < sets.length ; j++)
                assertFalse(sets[i].retainAll(sets[j]));
    }

    @Test
    public void testRemoveAll() {
        for (int i = 0; i < sets.length - 1; i++) {
            final ExtensionSet set = sets[i];
            assertFalse(set.removeAll(empty));
            assertFalse(set.isEmpty());
            assertTrue(set.removeAll(sets[i + 1]));
            assertTrue(set.isEmpty());
        }
    }

    @Test
    public void testClear() {
        for (final ExtensionSet set : sets) {
            assertFalse(set.isEmpty());
            set.clear();
            assertTrue(set.isEmpty());
        }
    }

    @Test
    public void testToString() {
        for (final ExtensionSet set : sets) {
            assertTrue("extension1|extension2".equals(set.toString()));
        }
    }

    @Test
    public void testToPattern() {
        for (final ExtensionSet set : sets) {
            assertTrue(set.toPattern().matcher(".extension1").matches());
            assertTrue(set.toPattern().matcher(".extension2").matches());
            assertTrue(set.toPattern().matcher("a.EXTENSION1").matches());
            assertTrue(set.toPattern().matcher("a.EXTENSION2").matches());
            assertTrue(set.toPattern().matcher("a.b.extension1").matches());
            assertTrue(set.toPattern().matcher("a.b.extension2").matches());
            assertTrue(set.toPattern().matcher("a.b.c.EXTENSION1").matches());
            assertTrue(set.toPattern().matcher("a.b.c.EXTENSION2").matches());
        }
    }
}