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

package de.schlichtherle.io;

import junit.framework.TestCase;

/**
 *
 * @author Christian Schlichtherle
 */
public class ChainableIOExceptionTest extends TestCase {
    
    public ChainableIOExceptionTest(String testName) {
        super(testName);
    }

    public void testSorting() {
        ArchiveException exc = null;
        final int max = 9;
        assertEquals(0, max % 3);
        final ChainableIOException[] appearance = new ArchiveException[max];
        final ChainableIOException[] revAppearance = new ArchiveException[max];
        final ChainableIOException[] priority = new ArchiveException[max];
        for (int i = 0; i < max; i++) {
            File dummy = (File) new File("" + i + 1).getAbsoluteFile();
            switch (i % 3) {
            case 0:
                exc = new ArchiveException(exc, dummy.getPath());
                break;
            
            case 1:
                exc = new ArchiveWarningException(exc, dummy.getPath());
                break;

            case 2:
                exc = new ArchiveBusyException(exc, dummy.getPath());
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
                //return ArchiveException.APPEARANCE_COMP.compare(e1, e2) == 0;
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

    private static interface Check {
        boolean equals(ChainableIOException e1, ChainableIOException e2);
    }
}
