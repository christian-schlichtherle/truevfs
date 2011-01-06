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
package de.schlichtherle.truezip.io.filesystem;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

/**
 * @author Christian Schlichtherle
 * @version $Id$
 */
public class FSManagersTest {

    @Test
    public void testInstance() {
        FSManagerTestCase.gc();

        final FSManager inst1 = FSManagers.getInstance();
        assertNotNull(inst1);
        assertThat(inst1.getSize(), is(0));

        FSManagers.setInstance(null);
        final FSManager inst2 = FSManagers.getInstance();
        assertNotNull(inst2);
        assertNotSame(inst1, inst2);

        final FSManager inst3 = new FSFederationManager();

        FSManagers.setInstance(inst3);
        final FSManager inst4 = FSManagers.getInstance();
        assertNotNull(inst4);
        assertSame(inst3, inst4);
    }
}
