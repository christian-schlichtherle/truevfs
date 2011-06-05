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
import de.schlichtherle.truezip.fs.FsMountPoint;
import de.schlichtherle.truezip.fs.archive.mock.MockArchiveDriver;
import static de.schlichtherle.truezip.nio.fsp.TFileSystemProvider.Parameter.*;
import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;

/**
 * @author  Christian Schlichtherle
 * @version $Id$
 */
public class TFileSystemProviderTest {

    private static final FsMountPoint
            CURRENT_DIRECTORY = FsMountPoint.create(Paths.get("").toUri());

    private Map<String, Object> environment;
    private TFileSystemProvider provider;
    
    @Before
    public void setUp() throws Exception {
        final TArchiveDetector
                detector = new TArchiveDetector("mok", new MockArchiveDriver());
        environment = new HashMap<>();
        environment.put(ARCHIVE_DETECTOR, detector);
        provider = TFileSystemProvider.File.class.newInstance();
    }

    @Test
    public void testNewFileSystemFromPath() {
        for (final Object[] params : new Object[][] {
            // $first, $more, $mountPoint
            //{ "foo.mok", new String[0], "mok:" + CURRENT_DIRECTORY + "foo.mok!/" },
            { "foo", new String[0], null },
        }) {
            Path path = Paths.get(params[0].toString(), (String[]) params[1]);
            FsMountPoint mp = null == params[2]
                    ? null
                    : FsMountPoint.create(URI.create(params[2].toString()));
            try {
                TFileSystem fs = provider.newFileSystem(path, environment);
                if (null == mp)
                    fail();
                assertThat(fs.getMountPoint(), is(mp));
            } catch (UnsupportedOperationException ex) {
                if (null != mp)
                    throw ex;
            }
        }
    }

    /*@Test
    public void testgetPath() {
        for (final String[] params : new String[][] {
            { "foo", },
        }) {
            URI uri = URI.create(params[0]);
            TPath path = provider.getPath(uri);
            assertThat(path.getFileSystem().provider(), is(provider));
        }
    }*/
}
