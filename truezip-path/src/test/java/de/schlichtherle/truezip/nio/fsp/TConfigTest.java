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
package de.schlichtherle.truezip.nio.fsp;

import de.schlichtherle.truezip.file.TArchiveDetector;
import de.schlichtherle.truezip.fs.archive.mock.MockArchiveDriver;
import static org.junit.Assert.*;
import org.junit.Test;
import static org.hamcrest.CoreMatchers.*;

/**
 * @author  Christian Schlichtherle
 * @version $Id$
 */
public class TConfigTest extends TestBase {
    @Test
    public void testNormalSessionUse() {
        try (TConfig session = TConfig.push()) {
            session.setArchiveDetector(new TArchiveDetector("mok", new MockArchiveDriver()));
            // Create some TFile or TPath objects here and do some I/O:
        }
    }

    @Test
    public void testWierdSessionUse() {
        TConfig session1 = TConfig.get();
        assertThat(TConfig.get(), sameInstance(session1));
        assertThat(session1.getArchiveDetector(), sameInstance(detector));
        final TArchiveDetector newGlobalDetector = new TArchiveDetector("mok", new MockArchiveDriver());
        try (TConfig session2 = TConfig.push()) {
            session1.setArchiveDetector(newGlobalDetector);
            assertThat(TConfig.get(), sameInstance(session2));
            assertThat(session2.getArchiveDetector(), sameInstance(detector));
            final TArchiveDetector newLocal1Detector = new TArchiveDetector("mok", new MockArchiveDriver());
            try (TConfig session3 = TConfig.push()) {
                session2.setArchiveDetector(newLocal1Detector);
                assertThat(TConfig.get(), sameInstance(session3));
                assertThat(session3.getArchiveDetector(), sameInstance(detector));
            }
            assertThat(TConfig.get(), sameInstance(session2));
            assertThat(session2.getArchiveDetector(), sameInstance(newLocal1Detector));
        }
        assertThat(TConfig.get(), sameInstance(session1));
        assertThat(session1.getArchiveDetector(), sameInstance(newGlobalDetector));
    }
}
