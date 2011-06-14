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
import de.schlichtherle.truezip.fs.FsPath;
import de.schlichtherle.truezip.fs.archive.mock.MockArchiveDriver;
import java.net.URI;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;
import static org.hamcrest.CoreMatchers.*;

/**
 * @author  Christian Schlichtherle
 * @version $Id$
 */
public class TScannerTest {
    
    private TArchiveDetector detector;

    @Before
    public void setUp() throws IOException {
        detector = new TArchiveDetector("mok", new MockArchiveDriver());
    }

    @Test
    public void testToFsPath() {
        for (final String[] params : new String[][] {
            // { $parent, $member, $path },
            { "foo", "..", "" },
            { "foo/bar", "../..", "" },
            { "scheme:/foo", "..", "scheme:/" },
            { "scheme:/foo/bar", "", "scheme:/foo/bar" },
            { "scheme:/foo/bar", "..", "scheme:/foo" },
            { "scheme:/foo/bar", "../..", "scheme:/" },
            { "scheme:/foo.mok/bar.mok", "../..", "scheme:/" },
            { "mok:mok:scheme:/foo.mok!/bar.mok!/", "", "mok:mok:scheme:/foo.mok!/bar.mok!/" },
            { "mok:mok:scheme:/foo.mok!/bar.mok!/", "..", "mok:scheme:/foo.mok!/" },
            { "mok:mok:scheme:/foo.mok!/bar.mok!/", "../..", "scheme:/" },
            { "mok:mok:scheme:/foo.mok!/x/bar.mok!/y", "", "mok:mok:scheme:/foo.mok!/x/bar.mok!/y" },
            { "mok:mok:scheme:/foo.mok!/x/bar.mok!/y", "..", "mok:mok:scheme:/foo.mok!/x/bar.mok!/" },
            { "mok:mok:scheme:/foo.mok!/x/bar.mok!/y", "../..", "mok:scheme:/foo.mok!/x" },
            { "mok:mok:scheme:/foo.mok!/x/bar.mok!/y", "../../..", "mok:scheme:/foo.mok!/" },
            { "mok:mok:scheme:/foo.mok!/x/bar.mok!/y", "../../../..", "scheme:/" },
            //{ "mok:mok:scheme:/foo.mok!/x/bar.mok!/y", "../../../../..", "null" },
        }) {
            final FsPath parent = FsPath.create(URI.create(params[0]));
            final URI member = URI.create(params[1]);
            final FsPath path = FsPath.create(URI.create(params[2]));
            final FsPath result = new TScanner(detector).toPath(parent, member);
            assertThat(result, equalTo(path));
        }
    }
}
