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
public class TSessionTest extends TestBase {
    @Test
    public void testSession() {
        TSession global = TSession.getSession();
        assertThat(TSession.getSession(), sameInstance(global));
        assertThat(global.getArchiveDetector(), sameInstance(detector));
        final TArchiveDetector newGlobalDetector = new TArchiveDetector("mok", new MockArchiveDriver());
        try (TSession local1 = TSession.newSession()) {
            global.setArchiveDetector(newGlobalDetector);
            assertThat(TSession.getSession(), sameInstance(local1));
            assertThat(local1.getArchiveDetector(), sameInstance(detector));
            final TArchiveDetector newLocal1Detector = new TArchiveDetector("mok", new MockArchiveDriver());
            try (TSession local2 = TSession.newSession()) {
                local1.setArchiveDetector(newLocal1Detector);
                assertThat(TSession.getSession(), sameInstance(local2));
                assertThat(local2.getArchiveDetector(), sameInstance(detector));
            }
            assertThat(TSession.getSession(), sameInstance(local1));
            assertThat(local1.getArchiveDetector(), sameInstance(newLocal1Detector));
        }
        assertThat(TSession.getSession(), sameInstance(global));
        assertThat(global.getArchiveDetector(), sameInstance(newGlobalDetector));
    }
}
