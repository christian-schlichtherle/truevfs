/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package de.schlichtherle.truezip.util;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Tests {@link SuffixSet}.
 *
 * @author Christian Schlichtherle
 * @version $Id$
 */
public final class SuffixSetTest {

    private SuffixSet empty;

    /** An array of equal suffix sets. */
    private SuffixSet[] sets;

    @Before
    public void setUp() {
        empty = new SuffixSet("");

        sets = new SuffixSet[] {
            new SuffixSet("suffix1|suffix2"),
            new SuffixSet("suffix2|suffix1"),
            new SuffixSet(".suffix1|.suffix2"),
            new SuffixSet(".suffix2|.suffix1"),
            new SuffixSet("SUFFIX1|SUFFIX2"),
            new SuffixSet("SUFFIX2|SUFFIX1"),
            new SuffixSet(".SUFFIX1|.SUFFIX2"),
            new SuffixSet(".SUFFIX2|.SUFFIX1"),
            new SuffixSet("SuFfIx1|SuFfIx2"),
            new SuffixSet("SuFfIx2|SuFfIx1"),
            new SuffixSet(".sUfFiX1|.sUfFiX2"),
            new SuffixSet(".sUfFiX2|.sUfFiX1"),
            new SuffixSet("suffix1|.suffix2|suffix2|.suffix1"),
            new SuffixSet("suffix2|.suffix1|suffix1|.suffix2"),
            new SuffixSet("SUFFIX1|.SUFFIX2|SUFFIX2|.SUFFIX1"),
            new SuffixSet("SUFFIX2|.SUFFIX1|SUFFIX1|.SUFFIX2"),
            new SuffixSet("SuFfIx1|.sUfFiX2|SuFfIx2|.sUfFiX1"),
            new SuffixSet("SuFfIx2|.sUfFiX1|SuFfIx1|.sUfFiX2"),
            new SuffixSet("suffix1|.suffix1|SUFFIX1|.SUFFIX1|suffix2|.suffix2|SUFFIX2|.SUFFIX2"),
            new SuffixSet("suffix2|.suffix2|SUFFIX2|.SUFFIX2|suffix1|.suffix1|SUFFIX1|.SUFFIX1"),
            new SuffixSet("suffix1|.suffix1|SUFFIX1|.SUFFIX1|SuFfIx1|.sUfFiX1|suffix2|.suffix2|SUFFIX2|.SUFFIX2|SuFfIx2|.sUfFiX2"),
            new SuffixSet("suffix2|.suffix2|SUFFIX2|.SUFFIX2|SuFfIx2|.sUfFiX2|suffix1|.suffix1|SUFFIX1|.SUFFIX1|SuFfIx1|.sUfFiX1"),
            new SuffixSet("suffix1||.suffix1||SUFFIX1||.SUFFIX1||SuFfIx1||.sUfFiX1||suffix2||.suffix2||SUFFIX2||.SUFFIX2||SuFfIx2||.sUfFiX2"),
            new SuffixSet("suffix2||.suffix2||SUFFIX2||.SUFFIX2||SuFfIx2||.sUfFiX2||suffix1||.suffix1||SUFFIX1||.SUFFIX1||SuFfIx1||.sUfFiX1"),
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

        for (final SuffixSet set : sets)
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
            final SuffixSet set = sets[i];
            assertFalse(set.removeAll(empty));
            assertFalse(set.isEmpty());
            assertTrue(set.removeAll(sets[i + 1]));
            assertTrue(set.isEmpty());
        }
    }

    @Test
    public void testClear() {
        for (final SuffixSet set : sets) {
            assertFalse(set.isEmpty());
            set.clear();
            assertTrue(set.isEmpty());
        }
    }

    @Test
    public void testToString() {
        for (final SuffixSet set : sets) {
            assertTrue("suffix1|suffix2".equals(set.toString()));
        }
    }

    @Test
    public void testToPattern() {
        for (int i = 0; i < sets.length; i++) {
            final SuffixSet set = sets[i];
            assertTrue(set.toPattern().matcher(".suffix1").matches());
            assertTrue(set.toPattern().matcher(".suffix2").matches());
            assertTrue(set.toPattern().matcher("a.SUFFIX1").matches());
            assertTrue(set.toPattern().matcher("a.SUFFIX2").matches());
            assertTrue(set.toPattern().matcher("a.b.suffix1").matches());
            assertTrue(set.toPattern().matcher("a.b.suffix2").matches());
            assertTrue(set.toPattern().matcher("a.b.c.SUFFIX1").matches());
            assertTrue(set.toPattern().matcher("a.b.c.SUFFIX2").matches());
        }
    }
}
