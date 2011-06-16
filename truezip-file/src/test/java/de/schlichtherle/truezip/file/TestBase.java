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
package de.schlichtherle.truezip.file;

import de.schlichtherle.truezip.fs.archive.mock.MockArchiveDriver;
import org.junit.After;
import org.junit.Before;

/**
 * @author  Christian Schlichtherle
 * @version $Id$
 */
public abstract class TestBase {

    private final TArchiveDetector detector;

    TestBase() {
        detector = new TArchiveDetector("mok|mok1|mok2", new MockArchiveDriver());
    }

    @Before
    public void setUp() throws Exception {
        final TConfig config = TConfig.push();
        config.setLenient(true);
        config.setArchiveDetector(detector);
    }

    @After
    public void tearDown() throws Exception {
        TConfig.pop();
    }
}
