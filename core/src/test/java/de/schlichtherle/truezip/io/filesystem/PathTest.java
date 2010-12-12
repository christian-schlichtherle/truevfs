/*
 * Copyright (C) 2010 Schlichtherle IT Services
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
package de.schlichtherle.truezip.io.filesystem;

import java.net.URI;
import java.net.URISyntaxException;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

/**
 * @author Christian Schlichtherle
 * @version $Id$
 */
public class PathTest {

    @Test
    @SuppressWarnings("ResultOfObjectAllocationIgnored")
    public void testConstructorWithInvalidUri() throws URISyntaxException {
        try {
            Path.create(null);
            fail();
        } catch (NullPointerException expected) {
        }

        try {
            new Path(null);
            fail();
        } catch (NullPointerException expected) {
        }

        try {
            Path.create(null, false);
            fail();
        } catch (NullPointerException expected) {
        }

        try {
            new Path(null, false);
            fail();
        } catch (NullPointerException expected) {
        }

        try {
            Path.create(null, true);
            fail();
        } catch (NullPointerException expected) {
        }

        try {
            new Path(null, true);
            fail();
        } catch (NullPointerException expected) {
        }

        try {
            new Path(null, null);
            fail();
        } catch (NullPointerException expected) {
        }

        for (final String param : new String[] {
            "/../foo#boo",
            "/../foo#",
            "/../foo",
            "/./foo",
            "//foo",
            "/foo",
            "/foo/bar",
            "/foo/bar/",
            "/",
            "foo//",
            "foo/.",
            "foo/./",
            "foo/..",
            "foo/../",
            "foo:bar",
            "foo:bar:",
            "foo:bar:/",
            "foo:bar:/baz",
            "foo:bar:/baz!",
            "foo:bar:/baz/",
            "foo:bar:/baz!//",
            "foo:bar:/baz!/#",
            "foo:bar:/baz!/#bang",
            "foo:bar:/baz!/.",
            "foo:bar:/baz!/./",
            "foo:bar:/baz!/..",
            "foo:bar:/baz!/../",
            "foo:bar:/baz!/bang/.",
            "foo:bar:/baz!/bang/./",
            "foo:bar:/baz!/bang/..",
            "foo:bar:/baz!/bang/../",
            "foo:bar:baz:/bang",
            "foo:bar:baz:/bang!",
            "foo:bar:baz:/bang/",
            "foo:bar:baz:/bang!/",
            "foo:bar:baz:/bang!/boom",
            "foo:bar:/baz/.!/",
            "foo:bar:/baz/./!/",
            "foo:bar:/baz/..!/",
            "foo:bar:/baz/../!/",

            "foo:bar:/baz/../!/bang/",
            "foo:bar:/baz/..!/bang/",
            "foo:bar:/baz/./!/bang/",
            "foo:bar:/baz/.!/bang/",
            "foo:bar:/../baz/!/bang/",
            "foo:bar:/./baz/!/bang/",
            "foo:bar://baz/!/bang/", // baz is authority!
            "foo:bar://baz!/bang/", // baz is authority!

            "foo:bar:/!/bang/",

            "foo:bar:/baz/../!/bang",
            "foo:bar:/baz/..!/bang",
            "foo:bar:/baz/./!/bang",
            "foo:bar:/baz/.!/bang",
            "foo:bar:/../baz/!/bang",
            "foo:bar:/./baz/!/bang",
            "foo:bar://baz/!/bang", // baz is authority!
            "foo:bar://baz!/bang", // baz is authority!

            "foo:bar:/!/bang",

            "foo:bar:/baz/!/",
            "foo:bar:/baz/?bang!/?plonk",
            "foo:bar:/baz//!/",
            "foo:bar:/baz/./!/",
            "foo:bar:/baz/..!/",
            "foo:bar:/baz/../!/",
        }) {
            final URI uri = URI.create(param);

            try {
                Path.create(uri);
                fail(param);
            } catch (IllegalArgumentException expected) {
            }

            try {
                new Path(uri);
                fail(param);
            } catch (URISyntaxException expected) {
            }
        }

        for (final String[] params : new String[][] {
            //{ "foo:/bar/", "baz" },
        }) {
            final MountPoint mountPoint = null == params[0] ? null : new MountPoint(URI.create(params[0]));
            final EntryName entryName = new EntryName(URI.create(params[1]));
            new Path(mountPoint, entryName);
            fail(params[0] + params[1]);
        }
    }

    @Test
    @SuppressWarnings("ResultOfObjectAllocationIgnored")
    public void testConstructorWithValidUri() throws URISyntaxException {
        for (final String[] params : new String[][] {
            { "foo:bar:baz:/bang!/boom!/plonk/", "foo:bar:baz:/bang!/boom!/", "plonk/" },
            { "foo:bar:baz:/bang!/boom!/plonk", "foo:bar:baz:/bang!/boom!/", "plonk" },
            { "foo:bar:baz:/bang!/boom!/", "foo:bar:baz:/bang!/boom!/", "" },

            { "foo:bar:/baz!/bang/../", "foo:bar:/baz!/", "" },
            { "foo:bar:/baz!/bang/..", "foo:bar:/baz!/", "" },
            { "foo:bar:/baz!/bang/./", "foo:bar:/baz!/", "bang/" },
            { "foo:bar:/baz!/bang/.", "foo:bar:/baz!/", "bang/" },

            { "foo:bar:/baz!/bang/", "foo:bar:/baz!/", "bang/" },

            { "foo:bar:/baz!/bang", "foo:bar:/baz!/", "bang" },

            { "foo:bar:/baz!/./", "foo:bar:/baz!/", "" },
            { "foo:bar:/baz!/.", "foo:bar:/baz!/", "" },
            { "foo:bar:/baz!/", "foo:bar:/baz!/", "" },
            { "foo:bar:/baz?bang!/?plonk", "foo:bar:/baz?bang!/", "?plonk" },

            { "foo:bar:/baz!/bang//", "foo:bar:/baz!/", "bang/" },
            { "foo:bar:/baz!/bang/.", "foo:bar:/baz!/", "bang/" },
            { "foo:bar:/baz!/bang/./", "foo:bar:/baz!/", "bang/" },
            { "foo:bar:/baz!/bang/..", "foo:bar:/baz!/", "" },
            { "foo:bar:/baz!/bang/../", "foo:bar:/baz!/", "" },

            { "foo", null, "foo" },
            { "foo/", null, "foo/" },
            { "foo//", null, "foo/" },
            { "foo/.", null, "foo/" },
            { "foo/./", null, "foo/" },
            { "foo/..", null, "" },
            { "foo/../", null, "" },
            { "foo/bar", null, "foo/bar" },
            { "foo/bar/", null, "foo/bar/" },
            { "foo:/", "foo:/", "" },
            { "foo:/bar", "foo:/", "bar" },
            { "foo:/bar/", "foo:/bar/", "" },
            { "foo:/bar//", "foo:/bar/", "" },
            { "foo:/bar/.", "foo:/bar/", "" },
            { "foo:/bar/./", "foo:/bar/", "" },
            { "foo:/bar/..", "foo:/", "" },
            { "foo:/bar/../", "foo:/", "" },
            { "foo:/bar/baz", "foo:/bar/", "baz" },
            { "foo:/bar/baz?bang", "foo:/bar/", "baz?bang" },
            { "foo:/bar/baz/", "foo:/bar/baz/", "" },
            { "foo:/bar/baz/?bang", "foo:/bar/baz/", "?bang" },
        }) {
            Path path = new Path(URI.create(params[0]), true);
            final MountPoint mountPoint = null == params[1] ? null : new MountPoint(URI.create(params[1]));
            final EntryName entryName = new EntryName(URI.create(params[2]));
            testPath(path, mountPoint, entryName);

            path = new Path(mountPoint, entryName);
            testPath(path, mountPoint, entryName);
        }
    }

    private void testPath(final Path path,
                          final MountPoint mountPoint,
                          final EntryName entryName) {
        if (null != mountPoint) {
            assertThat(path.getUri(), equalTo(URI.create(
                    mountPoint.getUri().toString()
                    + entryName.getUri().toString())));
            assertThat(path.getMountPoint().getUri(), equalTo(mountPoint.getUri()));
        } else {
            assertThat(path.getUri(), equalTo(entryName.getUri()));
            assertThat(path.getMountPoint(), nullValue());
        }
        assertThat(path.getEntryName().getUri(), equalTo(entryName.getUri()));
        assertThat(path.toString(), equalTo(path.getUri().toString()));
        assertThat(path, equalTo(Path.create(URI.create(path.getUri().toString()))));
        assertThat(path.hashCode(), equalTo(Path.create(URI.create(path.getUri().toString())).hashCode()));
    }
}
