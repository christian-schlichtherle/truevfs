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
public class FileSystemManagersTest {

    @Test
    public void testInstance() {
        FileSystemManagerTestCase.gc();
        final FileSystemManager inst1 = FileSystemManagers.getInstance();
        assertNotNull(inst1);
        assertThat(inst1.size(), is(0));

        FileSystemManagers.setInstance(null);
        final FileSystemManager inst2 = FileSystemManagers.getInstance();
        assertNotNull(inst2);
        assertNotSame(inst1, inst2);

        final FileSystemManager inst3 = new FederatedFileSystemManager();

        FileSystemManagers.setInstance(inst3);
        final FileSystemManager inst4 = FileSystemManagers.getInstance();
        assertNotNull(inst4);
        assertSame(inst3, inst4);
    }
}
