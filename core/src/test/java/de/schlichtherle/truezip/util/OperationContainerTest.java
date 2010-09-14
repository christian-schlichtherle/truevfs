/*
 * Copyright (C) 2010 Schlichtherle IT Services
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

package de.schlichtherle.truezip.util;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import junit.framework.TestCase;

/**
 * Tests OperationContainer.
 * 
 * @author Christian Schlichtherle
 * @version $Id$
 * @deprecated This feature is currently unused.
 */
public class OperationContainerTest extends TestCase {

    public void testRunSuccesses() {
        final int n = 2;
        final List<Success> successes = new ArrayList<Success>(
                Collections.nCopies(n, new Success()));
        final OperationHandler handler = new OperationHandler();
        final OperationContainer<RuntimeException> container
                = new OperationContainer<RuntimeException>(
                    successes, true, handler, IOException.class);
        container.run();
        assertTrue(successes.isEmpty());
        assertEquals(0, handler.warnCount);
    }

    public void testRunFailures() {
        final int n = 2;
        final List<Failure> failures = Collections.nCopies(n, new Failure());
        final OperationHandler handler = new OperationHandler();
        final OperationContainer<RuntimeException> container
                = new OperationContainer<RuntimeException>(
                    failures, true, handler, IOException.class);
        container.run();
        assertEquals(n, failures.size());
        assertEquals(n, handler.warnCount);
    }

    private static class Success implements Operation<IOException> {
        @Override
        public void run() {
        }
    }

    private static class Failure implements Operation<IOException> {
        @Override
        public void run() throws IOException {
            throw new FileNotFoundException("huh?");
        }
    }

    private static class OperationHandler
    implements ExceptionHandler<IOException, RuntimeException> {
        int warnCount;

        @Override
        public RuntimeException fail(IOException cannotHappen) {
            throw new AssertionError(cannotHappen);
        }

        @Override
        public void warn(IOException cause) {
            warnCount++;
        }
    }

}
