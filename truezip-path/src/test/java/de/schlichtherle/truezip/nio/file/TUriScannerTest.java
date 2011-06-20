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
package de.schlichtherle.truezip.nio.file;

import de.schlichtherle.truezip.file.TConfig;
import de.schlichtherle.truezip.fs.FsMountPoint;
import de.schlichtherle.truezip.fs.FsPath;
import java.net.URI;
import org.junit.Test;
import static org.junit.Assert.*;
import static org.hamcrest.CoreMatchers.*;

/**
 * @author  Christian Schlichtherle
 * @version $Id$
 */
public class TUriScannerTest extends TestBase {
    @Test
    public void testResolve() {
        for (final String[] params : new String[][] {
            // $parent, $member, $path, [$mountPoint]
            { "file:/", "c%3A/foo", "file:/c:/foo", "file:/c:/" },
            { "file:/", "//foo/bar/", "file://foo/bar/", "file://foo/bar/" },
            { "file:/", "//foo/bar", "file://foo/bar", "file://foo/" },
            { "foo", "bar", "foo/bar", null },
            { "foo", "..", "", null },
            { "foo/bar", "../..", "", null },
            { "scheme:/foo", "..", "scheme:/", "scheme:/" },
            { "scheme:/foo/bar", "", "scheme:/foo/bar", "scheme:/foo/" },
            { "scheme:/foo/bar", "..", "scheme:/foo/", "scheme:/foo/" },
            { "scheme:/foo/bar", "../..", "scheme:/", "scheme:/" },
            { "scheme:/foo.mok/bar.mok", "../..", "scheme:/", "scheme:/" },
            { "mok:mok:scheme:/foo.mok!/bar.mok!/", "", "mok:mok:scheme:/foo.mok!/bar.mok!/", "mok:mok:scheme:/foo.mok!/bar.mok!/" },
            { "mok:mok:scheme:/foo.mok!/bar.mok!/", "..", "mok:scheme:/foo.mok!/", "mok:scheme:/foo.mok!/" },
            { "mok:mok:scheme:/foo.mok!/bar.mok!/", "../..", "scheme:/", "scheme:/" },
            { "mok:mok:scheme:/foo.mok!/x/bar.mok!/y", "", "mok:mok:scheme:/foo.mok!/x/bar.mok!/y", "mok:mok:scheme:/foo.mok!/x/bar.mok!/" },
            { "mok:mok:scheme:/foo.mok!/x/bar.mok!/y", "..", "mok:mok:scheme:/foo.mok!/x/bar.mok!/", "mok:mok:scheme:/foo.mok!/x/bar.mok!/" },
            { "mok:mok:scheme:/foo.mok!/x/bar.mok!/y", "../..", "mok:scheme:/foo.mok!/x", "mok:scheme:/foo.mok!/" },
            { "mok:mok:scheme:/foo.mok!/x/bar.mok!/y", "../../..", "mok:scheme:/foo.mok!/", "mok:scheme:/foo.mok!/" },
            { "mok:mok:scheme:/foo.mok!/x/bar.mok!/y", "../../../..", "scheme:/", "scheme:/" },
            //{ "mok:mok:scheme:/foo.mok!/x/bar.mok!/y", "../../../../..", "null" },
        }) {
            final FsPath parent = FsPath.create(URI.create(params[0]));
            final URI member = URI.create(params[1]);
            final FsPath path = FsPath.create(URI.create(params[2]));
            final FsMountPoint mountPoint = null == params[3]
                    ? null
                    : FsMountPoint.create(URI.create(params[3]));
            final FsPath result = new TUriScanner(TConfig.get().getArchiveDetector()).resolve(parent, member);
            assertThat(result, equalTo(path));
            assertThat(result.getMountPoint(), is(mountPoint));
        }
    }
}
