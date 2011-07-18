/*
 * Copyright (C) 2006-2011 Schlichtherle IT Services
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
package de.schlichtherle.truezip.io;

import java.io.File;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Tests the class {@link SequentialIOException}.
 *
 * @author Christian Schlichtherle
 * @version $Id$
 */
public final class SequentialIOExceptionTest {

    @Test
    public void testSorting() {
        SequentialIOException exc = null;
        final int max = 9;
        assertEquals(0, max % 3);
        final SequentialIOException[] appearance = new SequentialIOException[max];
        final SequentialIOException[] priority = new SequentialIOException[max];
        for (int i = 0; i < max; i++) {
            final File dummy = new File("" + (i + 1)).getAbsoluteFile();
            exc = new TestException(dummy.getPath(), i % 3).initPredecessor(exc);
            appearance[max - 1 - i]
                    = priority[max - 1 - (i % 3) * (max / 3) - (i / 3)]
                    = exc;
        }

        final int maxIndex = exc.maxIndex;
        assertEquals(max - 1, maxIndex);
        final Check indexCheck = new Check() {
            @Override
			public boolean equals(SequentialIOException e1, SequentialIOException e2) {
                //return Exception0.INDEX_COMP.compare(e1, e2) == 0;
                return e1 == e2;
            }
        };
        assertChain(indexCheck, appearance, exc);

        final SequentialIOException appearanceExc = exc.sortAppearance();
        assertEquals(maxIndex, appearanceExc.maxIndex);
        assertChain(indexCheck, appearance, appearanceExc);

        final Check priorityCheck = new Check() {
            @Override
			public boolean equals(SequentialIOException e1, SequentialIOException e2) {
                return SequentialIOException.PRIORITY_COMP.compare(e1, e2) == 0;
            }
        };
        final SequentialIOException priorityExc = exc.sortPriority();
        assertNotSame(exc, priorityExc);
        assertEquals(maxIndex, priorityExc.maxIndex);
        assertChain(priorityCheck, priority, priorityExc);
    }

    private void assertChain(
            final Check c,
            final SequentialIOException[] expected,
            SequentialIOException exc) {
        assert c != null;
        for (int i = 0; i < expected.length; i++) {
            final SequentialIOException exp = expected[i];
            assertNotNull(exp);
            assertNotNull(exc);
            assertTrue(c.equals(exp, exc));
            exc = exc.getPredecessor();
        }
        assertNull(exc);
    }

    private interface Check {
        boolean equals(SequentialIOException e1, SequentialIOException e2);
    }

    private static class TestException extends SequentialIOException {
        private static final long serialVersionUID = 4893204620357369739L;

        TestException(String message, int priority) {
            super(message, priority);
        }
    }

    @Test
    public void testInitPredecessor() {
        SequentialIOException exc1, exc2;

        exc1 = new SequentialIOException();
        exc2 = new SequentialIOException();

        try {
            exc1.initPredecessor(exc1);
            fail("A chainable exception can't be the predecessor of itself!");
        } catch (IllegalArgumentException expected) {
        }

        try {
            exc2.initPredecessor(exc1);
            fail("A chainable exception's predecessor's predecessor must be initialized!");
        } catch (IllegalArgumentException expected) {
        }

        exc1.initPredecessor(null);
        exc1.initPredecessor(null);

        try {
            exc1.initPredecessor(exc2);
            fail("Predecessor reinitialization not allowed!");
        } catch (IllegalStateException expected) {
        }

        exc2.initPredecessor(exc1);
        exc2.initPredecessor(exc1);

        try {
            exc2.initPredecessor(null);
            fail("Predecessor reinitialization not allowed!");
        } catch (IllegalStateException expected) {
        }
    }
}
