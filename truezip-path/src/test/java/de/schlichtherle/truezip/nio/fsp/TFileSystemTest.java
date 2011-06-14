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

import java.io.IOException;
import de.schlichtherle.truezip.file.TArchiveDetector;
import de.schlichtherle.truezip.fs.FsMountPoint;
import de.schlichtherle.truezip.fs.archive.mock.MockArchiveDriver;
import java.net.URI;
import java.nio.file.FileSystems;
import java.util.HashMap;
import java.util.Map;
import static org.hamcrest.CoreMatchers.*;
import org.junit.After;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * @author  Christian Schlichtherle
 * @version $Id$
 */
public class TFileSystemTest {

    private static TArchiveDetector detectorBackup;
    
    private Map<String, Object> map;

    @BeforeClass
    public static void setUpClass() {
        detectorBackup = TPath.getDefaultArchiveDetector();
    }

    @Before
    public void setUp() throws IOException {
        final TArchiveDetector
                detector = new TArchiveDetector("mok", new MockArchiveDriver());
        TPath.setDefaultArchiveDetector(detector);
        map = new HashMap<>();
        map.put(TFileSystemProvider.Parameter.ARCHIVE_DETECTOR, detector);
    }

    @After
    public void tearDown() {
        TPath.setDefaultArchiveDetector(detectorBackup);
    }

    @Test
    public void testNewFileSystem() throws IOException {
        for (String[] params : new String[][] {
            // { $uri, $mountPoint },
            { "truezip:/", "file:/" },
            { "truezip:///", "file:/" },
            { "truezip:/foo", "file:/" },
            { "truezip:/foo/", "file:/" },
            { "truezip:/foo/bar", "file:/foo/" },
            { "truezip:/foo/bar/", "file:/foo/" },
        }) {
            final URI uri = URI.create(params[0]);
            final FsMountPoint mountPoint = FsMountPoint.create(URI.create(params[1]));
            final TFileSystem fs = (TFileSystem) FileSystems.newFileSystem(uri, map);
            fs.close();
            assertThat(fs.isOpen(), is(true));
            assertThat(fs.getController().getModel().getMountPoint(), is(mountPoint));
        }
    }
}
