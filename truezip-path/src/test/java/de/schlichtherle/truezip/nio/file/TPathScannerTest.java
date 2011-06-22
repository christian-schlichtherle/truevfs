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
import static de.schlichtherle.truezip.nio.file.TPathScanner.*;
import static java.io.File.*;
import java.net.URI;
import java.net.URISyntaxException;
import org.junit.Test;
import static org.junit.Assert.*;
import static org.hamcrest.CoreMatchers.*;

/**
 * @author  Christian Schlichtherle
 * @version $Id$
 */
public class TPathScannerTest extends TestBase {
    @Test
    public void testScan() throws URISyntaxException {
        if ('\\' == separatorChar) {
            for (final String[] params : new String[][] {
                // $parent, $member, $path, [$mountPoint]
                { "file:/", "//foo/bar", "file://foo/bar", "file://foo/" },
                { "file:/", "//foo/bar/", "file://foo/bar/", "file://foo/bar/" },
                { "file:/", "c%3A/foo", "file:/c:/foo", "file:/c:/" },
                { "file:/", "c%3A/foo", "file:/c:/foo", "file:/c:/" },
                { "file:///c:/", "//foo/bar", "file://foo/bar", "file://foo/" },
                { "file:///c:/", "//foo/bar/", "file://foo/bar/", "file://foo/bar/" },
                { "file:///c:/", "c%3A/foo", "file:/c:/foo", "file:/c:/" },
                { "file:///c:/", "c%3A/foo", "file:/c:/foo", "file:/c:/" },
                { "file://host/share/", "//foo/bar", "file://foo/bar", "file://foo/" },
                { "file://host/share/", "//foo/bar/", "file://foo/bar/", "file://foo/bar/" },
                { "file://host/share/", "c%3A/foo", "file:/c:/foo", "file:/c:/" },
                { "file://host/share/", "c%3A/foo", "file:/c:/foo", "file:/c:/" },
            }) {
                assertScan(params);
            }
        }
        for (final String[] params : new String[][] {
            // $parent, $member, $path, [$mountPoint]
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
            assertScan(params);
        }
    }

    private static void assertScan(final String... params) throws URISyntaxException {
        final FsPath parent = new FsPath(new URI(params[0]));
        final URI member = new URI(params[1]);
        final FsPath path = new FsPath(new URI(params[2]));
        final FsMountPoint mountPoint = null == params[3]
                ? null
                : new FsMountPoint(new URI(params[3]));
        final FsPath result = new TPathScanner(
                    TConfig.get().getArchiveDetector())
                .scan(parent, member);
        assertThat(result, equalTo(path));
        assertThat(result.getMountPoint(), is(mountPoint));
    }

    @Test
    public void testParent() throws URISyntaxException {
        for (String[] params : new String[][] {
            // $path, $parent
            { "", null },
            { "foo", "" },
            { "file:/", null },
            { "file:/foo", "file:/" },
            { "file:/foo/", "file:/" },
            { "file:/foo/bar", "file:/foo/" },
            { "file:/foo/bar/", "file:/foo/" },
            { "mok:file:/foo!/", "file:/" },
            { "mok:file:/foo!/bar", "mok:file:/foo!/" },
            { "mok:mok:file:/foo!/bar!/", "mok:file:/foo!/" },
            { "mok:mok:file:/foo!/bar!/baz", "mok:mok:file:/foo!/bar!/" },
            { "mok:mok:file:/foo!/bar!/baz/boom", "mok:mok:file:/foo!/bar!/baz" },
        }) {
            final FsPath path = new FsPath(new URI(params[0]));
            final FsPath parent = params[1] == null
                    ? null
                    : new FsPath(new URI(params[1]));
            assertThat(parent(path), is(parent));
        }
    }
}
