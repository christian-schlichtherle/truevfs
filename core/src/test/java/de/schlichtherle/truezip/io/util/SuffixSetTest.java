/*
 * Copyright (C) 2007-2010 Schlichtherle IT Services
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

package de.schlichtherle.truezip.io.util;

import java.util.Iterator;
import java.util.regex.Pattern;
import junit.framework.TestCase;

/**
 * Tests {@link SuffixSet}.
 *
 * @author Christian Schlichtherle
 * @version $Id$
 */
public class SuffixSetTest extends TestCase {

    private SuffixSet empty;

    /** A set of equal canoncical string sets. */
    private SuffixSet[] sets;

    public SuffixSetTest(String testName) {
        super(testName);
    }

    @Override
    protected void setUp() {
        empty = new SuffixSet("");

        sets = new SuffixSet[] {
            new SuffixSet("suffix"),
            new SuffixSet(".suffix"),
            new SuffixSet("SUFFIX"),
            new SuffixSet(".SUFFIX"),
            new SuffixSet("SuFfIx"),
            new SuffixSet(".sUfFiX"),
            new SuffixSet("suffix|.suffix"),
            new SuffixSet("SUFFIX|.SUFFIX"),
            new SuffixSet("SuFfIx|.sUfFiX"),
            new SuffixSet("suffix|.suffix|SUFFIX|.SUFFIX"),
            new SuffixSet("suffix|.suffix|SUFFIX|.SUFFIX|SuFfIx|.sUfFiX"),
            new SuffixSet("suffix||.suffix||SUFFIX||.SUFFIX||SuFfIx||.sUfFiX"),
        };
    }

    public void testEqualsAndHashCode() {
        for (int i = 0; i < sets.length; i++) {
            for (int j = 0; j < sets.length ; j++) {
                assertTrue(sets[i].equals(sets[j]));
                assertEquals(sets[i].hashCode(), sets[j].hashCode());
            }
        }
    }

    public void testIsEmpty() {
        assertTrue(empty.isEmpty());

        for (int i = 0; i < sets.length; i++) {
            assertFalse(sets[i].isEmpty());
        }
    }

    public void testCanonicalIteratorAndContains() {
        assertFalse(empty.iterator().hasNext());

        for (int i = 0; i < sets.length; i++) {
            for (int j = 0; j < sets.length ; j++) {
                for (final Iterator it = sets[i].iterator(); it.hasNext();) {
                    final String s = (String) it.next();
                    assertTrue(sets[j].contains(s));
                }
            }
        }
    }

    public void testOriginalIteratorAndContains() {
        assertFalse(empty.originalIterator().hasNext());

        for (int i = 0; i < sets.length; i++) {
            for (int j = 0; j < sets.length ; j++) {
                for (final Iterator it = sets[i].originalIterator(); it.hasNext();) {
                    final String s = (String) it.next();
                    assertTrue(sets[j].contains(s));
                }
            }
        }
    }

    public void testAddAll() {
        for (int i = 0; i < sets.length; i++) {
            for (int j = 0; j < sets.length ; j++) {
                assertFalse(sets[i].addAll(sets[j]));
            }
        }
    }

    public void testRetainAll() {
        for (int i = 0; i < sets.length; i++) {
            for (int j = 0; j < sets.length ; j++) {
                assertFalse(sets[i].retainAll(sets[j]));
            }
        }
    }

    public void testRemoveAll() {
        for (int i = 0; i < sets.length - 1; i++) {
            final SuffixSet set = sets[i];
            assertFalse(set.removeAll(empty));
            assertFalse(set.isEmpty());
            assertTrue(set.removeAll(sets[i + 1]));
            assertTrue(set.isEmpty());
        }
    }

    public void testClear() {
        for (int i = 0; i < sets.length; i++) {
            final SuffixSet set = sets[i];
            assertFalse(set.isEmpty());
            set.clear();
            assertTrue(set.isEmpty());
        }
    }

    public void testToString() {
        for (int i = 0; i < sets.length; i++) {
            final SuffixSet set = sets[i];
            assertTrue("suffix".equals(set.toString()));
        }
    }

    public void testToRegex() {
        for (int i = 0; i < sets.length; i++) {
            final SuffixSet set = sets[i];
            assertTrue(Pattern.matches(set.toRegex(), ".suffix"));
            assertTrue(Pattern.matches(set.toRegex(), "a.SUFFIX"));
            assertTrue(Pattern.matches(set.toRegex(), "a.b.suffix"));
            assertTrue(Pattern.matches(set.toRegex(), "a.b.c.SUFFIX"));
        }
    }
}
