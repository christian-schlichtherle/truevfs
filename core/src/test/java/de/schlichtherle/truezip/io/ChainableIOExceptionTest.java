/*
 * Copyright (C) 2006-2010 Schlichtherle IT Services
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

import de.schlichtherle.truezip.io.ChainableIOException;
import java.io.File;
import junit.framework.TestCase;

/**
 * Tests the class {@link ChainableIOException}.
 *
 * @author Christian Schlichtherle
 * @version $Id$
 */
public class ChainableIOExceptionTest extends TestCase {
    
    public ChainableIOExceptionTest(String testName) {
        super(testName);
    }

    public void testSorting() {
        ChainableIOException exc = null;
        final int max = 9;
        assertEquals(0, max % 3);
        final ChainableIOException[] appearance = new ChainableIOException[max];
        final ChainableIOException[] priority = new ChainableIOException[max];
        for (int i = 0; i < max; i++) {
            final File dummy = (File) new File("" + (i + 1)).getAbsoluteFile();
            exc = new TestException(dummy.getPath(), i % 3).initPredecessor(exc);
            appearance[max - 1 - i]
                    = priority[max - 1 - (i % 3) * (max / 3) - (i / 3)]
                    = exc;
        }

        final int maxIndex = exc.maxIndex;
        assertEquals(max - 1, maxIndex);
        final Check indexCheck = new Check() {
            public boolean equals(ChainableIOException e1, ChainableIOException e2) {
                //return Exception0.INDEX_COMP.compare(e1, e2) == 0;
                return e1 == e2;
            }
        };
        testChain(indexCheck, appearance, exc);

        final ChainableIOException appearanceExc = exc.sortIndex();
        assertEquals(maxIndex, appearanceExc.maxIndex);
        testChain(indexCheck, appearance, appearanceExc);

        final Check priorityCheck = new Check() {
            public boolean equals(ChainableIOException e1, ChainableIOException e2) {
                return ChainableIOException.PRIORITY_COMP.compare(e1, e2) == 0;
            }
        };
        final ChainableIOException priorityExc = exc.sortPriority();
        assertNotSame(exc, priorityExc);
        assertEquals(maxIndex, priorityExc.maxIndex);
        testChain(priorityCheck, priority, priorityExc);
    }

    private void testChain(
            final Check c,
            final ChainableIOException[] expected,
            ChainableIOException exc) {
        assert c != null;
        for (int i = 0; i < expected.length; i++) {
            final ChainableIOException exp = expected[i];
            assertNotNull(exp);
            assertNotNull(exc);
            assertTrue(c.equals(exp, exc));
            exc = exc.getPredecessor();
        }
        assertNull(exc);
    }

    interface Check {
        boolean equals(ChainableIOException e1, ChainableIOException e2);
    }

    static class TestException extends ChainableIOException {
        private static final long serialVersionUID = 4893204620357369739L;

        TestException(String message, int priority) {
            super(message, priority);
        }
    }

    public void testInitPredecessor() {
        ChainableIOException exc1, exc2;

        exc1 = new ChainableIOException();
        exc2 = new ChainableIOException();

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

        try {
            exc1.initPredecessor(null);
            fail("Predecessor reinitialization not allowed!");
        } catch (IllegalStateException expected) {
        }

        exc2.initPredecessor(exc1);

        try {
            exc2.initPredecessor(exc1);
            fail("Predecessor reinitialization not allowed!");
        } catch (IllegalStateException expected) {
        }
    }
}
