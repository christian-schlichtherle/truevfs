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

package de.schlichtherle.truezip.io.util;

import de.schlichtherle.truezip.io.File;
import de.schlichtherle.truezip.io.util.ChainableIOException;
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
        final ChainableIOException[] revAppearance = new ChainableIOException[max];
        final ChainableIOException[] priority = new ChainableIOException[max];
        for (int i = 0; i < max; i++) {
            File dummy = (File) new File("" + i + 1).getAbsoluteFile();
            switch (i % 3) {
            case 0:
                exc = new Exception0(exc, dummy.getPath());
                break;
            
            case 1:
                exc = new Exception1(exc, dummy.getPath());
                break;

            case 2:
                exc = new Exception2(exc, dummy.getPath());
                break;
            }
            appearance[i]
                    = revAppearance[max - 1 - i]
                    = priority[(i % 3) * (max / 3) + (2 - i / 3)]
                    = exc;
        }

        final int maxAppearance = exc.maxAppearance;
        final Check appearanceCheck = new Check() {
            public boolean equals(ChainableIOException e1, ChainableIOException e2) {
                //return Exception0.APPEARANCE_COMP.compare(e1, e2) == 0;
                return e1 == e2;
            }
        };
        testChain(appearanceCheck, revAppearance, exc);

        final ChainableIOException appearanceExc = exc.sortAppearance();
        assertEquals(maxAppearance, appearanceExc.maxAppearance);
        testChain(appearanceCheck, revAppearance, appearanceExc);

        final Check priorityCheck = new Check() {
            public boolean equals(ChainableIOException e1, ChainableIOException e2) {
                return ChainableIOException.PRIORITY_COMP.compare(e1, e2) == 0;
            }
        };
        final ChainableIOException priorityExc = exc.sortPriority();
        assertNotSame(exc, priorityExc);
        assertEquals(maxAppearance, priorityExc.maxAppearance);
        testChain(priorityCheck, priority, priorityExc);
    }

    private void testChain(
            final Check c,
            final ChainableIOException[] expected,
            ChainableIOException exc) {
        assertNotNull(c);
        for (int i = 0; i < expected.length; i++) {
            final ChainableIOException exp = expected[i];
            assertNotNull(exp);
            assertNotNull(exc);
            assertTrue(c.equals(exp, exc));
            exc = exc.getPrior();
        }
        assertNull(exc);
    }

    interface Check {
        boolean equals(ChainableIOException e1, ChainableIOException e2);
    }

    static class Exception0 extends ChainableIOException {
        private static final long serialVersionUID = 4893204620357369739L;

        Exception0(ChainableIOException priorException, String message) {
            super(priorException, message);
        }
    }

    static class Exception1 extends ChainableIOException {
        private static final long serialVersionUID = 2302357394858347366L;

        Exception1(ChainableIOException priorException, String message) {
            super(priorException, message);
        }

        @Override
        public int getPriority() {
            return -1;
        }
    }

    static class Exception2 extends ChainableIOException {
        private static final long serialVersionUID = 1937861953461235716L;

        Exception2(ChainableIOException priorException, String cPath) {
            super(priorException, cPath);
        }

        @Override
        public int getPriority() {
            return -2;
        }
    }
}
