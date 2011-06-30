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
import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.DefaultAnnotation;
import edu.umd.cs.findbugs.annotations.NonNull;
import org.junit.After;
import org.junit.Before;

/**
 * @author  Christian Schlichtherle
 * @version $Id$
 */
@DefaultAnnotation(NonNull.class)
public abstract class TestBase {

    private TArchiveDetector detector;

    protected TestBase() {
        this(null);
    }

    protected TestBase(final @CheckForNull TArchiveDetector detector) {
        this.detector = null != detector
                ? detector
                : new TArchiveDetector("mok|mok1|mok2", new MockArchiveDriver());
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

    protected final TArchiveDetector getDetector() {
        return detector;
    }
}
