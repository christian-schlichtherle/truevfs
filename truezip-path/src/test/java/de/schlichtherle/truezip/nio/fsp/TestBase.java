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

import de.schlichtherle.truezip.fs.FsMountPoint;
import java.io.File;
import de.schlichtherle.truezip.file.TArchiveDetector;
import de.schlichtherle.truezip.fs.archive.mock.MockArchiveDriver;
import static de.schlichtherle.truezip.nio.fsp.TFileSystemProvider.Parameter.*;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;

/**
 * @author  Christian Schlichtherle
 * @version $Id$
 */
public abstract class TestBase {

    public static final FsMountPoint
            ROOT_DIRECTORY = FsMountPoint.create(URI.create("file:/"));
    public static final FsMountPoint
            CURRENT_DIRECTORY = FsMountPoint.create(new File("").toURI());
    public static final String[] NO_MORE = new String[0];

    protected TArchiveDetector detector;
    protected Map<String, Object> environment;

    @BeforeClass
    public static void setUpClass() {
        TConfig.push();
    }

    @AfterClass
    public static void tearDownClass() {
        TConfig.get().close();
    }

    @Before
    public void setUp() {
        detector = new TArchiveDetector("mok", new MockArchiveDriver());
        environment = new HashMap<>();
        environment.put(ARCHIVE_DETECTOR, detector);
        TConfig.get().setArchiveDetector(detector);
    }
}
