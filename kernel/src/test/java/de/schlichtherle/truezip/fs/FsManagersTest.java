/*
 * Copyright (C) 2011 Schlichtherle IT Services
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
package de.schlichtherle.truezip.fs;

import de.schlichtherle.truezip.fs.FsManager;
import de.schlichtherle.truezip.fs.FsManagers;
import de.schlichtherle.truezip.fs.FsFederatingManager;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

/**
 * @author Christian Schlichtherle
 * @version $Id$
 */
public class FsManagersTest {

    @Test
    public void testInstance() {
        FsManagerTestCase.gc();

        final FsManager inst1 = FsManagers.getInstance();
        assertNotNull(inst1);
        assertThat(inst1.getSize(), is(0));

        FsManagers.setInstance(null);
        final FsManager inst2 = FsManagers.getInstance();
        assertNotNull(inst2);
        assertNotSame(inst1, inst2);

        final FsManager inst3 = new FsFederatingManager();

        FsManagers.setInstance(inst3);
        final FsManager inst4 = FsManagers.getInstance();
        assertNotNull(inst4);
        assertSame(inst3, inst4);
    }
}
