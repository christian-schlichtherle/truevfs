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
import de.schlichtherle.truezip.fs.FsPath;
import de.schlichtherle.truezip.fs.archive.mock.MockArchiveDriver;
import static de.schlichtherle.truezip.nio.fsp.TestUtils.*;
import java.net.URI;
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
public class TPathTest {

    private static TArchiveDetector detectorBackup;

    @BeforeClass
    public static void setUpClass() {
        detectorBackup = TPath.getDefaultArchiveDetector();
    }

    @Before
    public void setUp() throws IOException {
        TPath.setDefaultArchiveDetector(
                new TArchiveDetector("mok", new MockArchiveDriver()));
    }

    @After
    public void tearDown() {
        TPath.setDefaultArchiveDetector(detectorBackup);
    }

    @Test
    public void testConstructorWithStrings() {
        for (Object[] params : new Object[][] {
            // { $detector, $first, $more, $path, $address },
            { null, "/", NO_MORE, "/", ROOT_DIRECTORY },
            { null, "/foo", NO_MORE, "/foo", ROOT_DIRECTORY + "foo" },
            { null, "/foo", new String[] { "" }, "/foo", ROOT_DIRECTORY + "foo"},
            { null, "/foo", new String[] { "bar" }, "/foo/bar", ROOT_DIRECTORY + "foo/bar"},
            { null, "//", NO_MORE, "/", ROOT_DIRECTORY },
            { null, "///", NO_MORE, "/", ROOT_DIRECTORY },
            { null, "//foo", new String[] { "bar", "baz" }, "//foo/bar/baz", ROOT_DIRECTORY + "/foo/bar/baz" },
            { null, "///foo//", new String[] { "//bar//", "//", "//baz//" }, "//foo/bar/baz", ROOT_DIRECTORY + "/foo/bar/baz" },
            { null, "/foo", new String[] { "/bar" }, "/foo/bar", ROOT_DIRECTORY + "foo/bar"},
            { null, "/foo//", new String[] { "//", "//bar//", "" }, "/foo/bar", ROOT_DIRECTORY + "foo/bar"},
            { null, "/foo", new String[] { "" }, "/foo", ROOT_DIRECTORY + "foo"},
            { null, "/foo", new String[] { ".." }, "/foo/..", ROOT_DIRECTORY },
            { null, "/foo", new String[] { "/bar" }, "/foo/bar", ROOT_DIRECTORY + "foo/bar" },
            { null, "/foo.mok", new String[] { "/bar" }, "/foo.mok/bar", "mok:" + ROOT_DIRECTORY + "foo.mok!/bar" },
            { null, "/foo", new String[] { "/bar.mok" }, "/foo/bar.mok", "mok:" + ROOT_DIRECTORY + "foo/bar.mok!/" },
            { null, "/foo.mok", new String[] { "/bar.mok" }, "/foo.mok/bar.mok", "mok:mok:" + ROOT_DIRECTORY + "foo.mok!/bar.mok!/" },
            { null, "", NO_MORE, "", CURRENT_DIRECTORY },
            { null, ".", NO_MORE, ".", CURRENT_DIRECTORY },
            { null, "foo", NO_MORE, "foo", CURRENT_DIRECTORY + "foo" },
            { null, "foo", new String[] { "" }, "foo", CURRENT_DIRECTORY + "foo"},
            { null, "foo", new String[] { ".." }, "foo/..", CURRENT_DIRECTORY },
            { null, "foo", new String[] { "bar" }, "foo/bar", CURRENT_DIRECTORY + "foo/bar" },
            { null, "foo.mok", new String[] { "bar" }, "foo.mok/bar", "mok:" + CURRENT_DIRECTORY + "foo.mok!/bar" },
            { null, "foo", new String[] { "bar.mok" }, "foo/bar.mok", "mok:" + CURRENT_DIRECTORY + "foo/bar.mok!/" },
            { null, "foo.mok", new String[] { "bar.mok" }, "foo.mok/bar.mok", "mok:mok:" + CURRENT_DIRECTORY + "foo.mok!/bar.mok!/" },
        }) {
            final TArchiveDetector detector = (TArchiveDetector) params[0];
            final String first = params[1].toString();
            final String[] more = (String[]) params[2];
            final URI uri = URI.create(params[3].toString());
            final FsPath address = FsPath.create(URI.create(params[4].toString()));
            final TPath path = new TPath(detector, first, more);
            assertThat(path.getUri(), equalTo(uri));
            assertThat(path.getAddress(), equalTo(address));
            assertThat(path.getFileSystem().getController().getModel().getMountPoint(), equalTo(path.getAddress().getMountPoint()));
        }
    }

    @Test
    public void testResolve() {
        for (Object[] params : new Object[][] {
            // { $parent, $detector, $first, $more, $label, $address },
            { "", null, "/", NO_MORE, "/", ROOT_DIRECTORY },
            { "x", null, "/foo", NO_MORE, "/foo", ROOT_DIRECTORY + "foo" },
            { "x", null, "/foo", new String[] { "" }, "/foo", ROOT_DIRECTORY + "foo"},
            { "x", null, "/foo", new String[] { "bar" }, "/foo/bar", ROOT_DIRECTORY + "foo/bar"},
            { "x", null, "//", NO_MORE, "/", ROOT_DIRECTORY },
            { "x", null, "///", NO_MORE, "/", ROOT_DIRECTORY },
            { "x", null, "//foo", new String[] { "bar", "baz" }, "//foo/bar/baz", ROOT_DIRECTORY + "/foo/bar/baz" },
            { "x", null, "///foo//", new String[] { "//bar//", "//", "//baz//" }, "//foo/bar/baz", ROOT_DIRECTORY + "/foo/bar/baz" },
            { "x", null, "/foo", new String[] { "/bar" }, "/foo/bar", ROOT_DIRECTORY + "foo/bar"},
            { "x", null, "/foo//", new String[] { "//", "//bar//", "" }, "/foo/bar", ROOT_DIRECTORY + "foo/bar"},
            { "x", null, "/foo", new String[] { "" }, "/foo", ROOT_DIRECTORY + "foo"},
            { "x", null, "/foo", new String[] { ".." }, "/foo/..", ROOT_DIRECTORY },
            { "x", null, "/foo", new String[] { "/bar" }, "/foo/bar", ROOT_DIRECTORY + "foo/bar" },
            { "x", null, "/foo.mok", new String[] { "/bar" }, "/foo.mok/bar", "mok:" + ROOT_DIRECTORY + "foo.mok!/bar" },
            { "x", null, "/foo", new String[] { "/bar.mok" }, "/foo/bar.mok", "mok:" + ROOT_DIRECTORY + "foo/bar.mok!/" },
            { "x", null, "/foo.mok", new String[] { "/bar.mok" }, "/foo.mok/bar.mok", "mok:mok:" + ROOT_DIRECTORY + "foo.mok!/bar.mok!/" },
            { "/", null, "", NO_MORE, "/", ROOT_DIRECTORY },
            { "/", null, "foo", NO_MORE, "/foo", ROOT_DIRECTORY + "foo" },
            { "/", null, ".", NO_MORE, "/", ROOT_DIRECTORY },
            { "", null, "bar", NO_MORE, "bar", CURRENT_DIRECTORY + "bar" },
            { ".", null, "bar", NO_MORE, "bar", CURRENT_DIRECTORY + "bar" },
            { "foo", null, "bar", NO_MORE, "foo/bar", CURRENT_DIRECTORY + "foo/bar" },
            { "foo", null, "bar", new String[] { "" }, "foo/bar", CURRENT_DIRECTORY + "foo/bar"},
            { "", null, "bar", new String[] { ".." }, "bar/..", CURRENT_DIRECTORY },
            { "foo.mok", null, "bar", NO_MORE, "foo.mok/bar", "mok:" + CURRENT_DIRECTORY + "foo.mok!/bar" },
            { "foo", null, "bar.mok", NO_MORE, "foo/bar.mok", "mok:" + CURRENT_DIRECTORY + "foo/bar.mok!/" },
            { "foo.mok", null, "bar.mok", NO_MORE, "foo.mok/bar.mok", "mok:mok:" + CURRENT_DIRECTORY + "foo.mok!/bar.mok!/" },
            { "foo.mok", null, "..", NO_MORE, "", CURRENT_DIRECTORY },
            { "foo.mok", null, "..", new String[] { "bar.mok" }, "bar.mok", "mok:" + CURRENT_DIRECTORY + "bar.mok!/"},
        }) {
            final TPath parent = new TPath(params[0].toString());
            final TArchiveDetector detector = (TArchiveDetector) params[1];
            final String first = params[2].toString();
            final String[] more = (String[]) params[3];
            final URI label = URI.create(params[4].toString());
            final FsPath address = FsPath.create(URI.create(params[5].toString()));
            final TPath member = new TPath(detector, first, more);
            final TPath path = parent.resolve(member);
            assertThat(path.getUri(), equalTo(label));
            assertThat(path.getAddress(), equalTo(address));
            assertThat(path.getFileSystem().getController().getModel().getMountPoint(), equalTo(path.getAddress().getMountPoint()));
        }
    }
}
