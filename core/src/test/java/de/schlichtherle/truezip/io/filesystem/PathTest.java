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
    public void testConstructorWithUri() throws URISyntaxException {
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

        for (final String param : new String[] {
            "foo:bar",
            "foo:bar:",
            "foo:bar:/",
            "foo:bar:/baz",
            "foo:bar:/baz!",
            "foo:bar:/baz/",
            "foo:bar:/baz!//",
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
            "foo:bar:/baz/.!/",
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
            "foo",
            "foo/",
            "foo//",
            "foo/.",
            "foo/./",
            "foo/..",
            "foo/../",
            "foo/bar",
            "foo/bar/",
            "foo:/bar",
            "foo:/bar/",
            "foo:/bar//",
            "foo:/bar/.",
            "foo:/bar/./",
            "foo:/bar/..",
            "foo:/bar/../",
            "foo:/bar/baz",
            "foo:/bar/baz/",
        }) {
            final URI uri = new URI(param);
            final Path path = new Path(uri);
            assertThat(path.getUri(), sameInstance(uri));
            assertThat(path.getMountPoint(), nullValue());
            assertThat(path.getEntryName(), nullValue());
            assertThat(path.toString(), equalTo(path.getUri().toString()));
            assertThat(path, equalTo(path));
            assertThat(path.hashCode(), equalTo(path.hashCode()));
        }

        for (final String[] params : new String[][] {
            { "foo:bar:baz:/bang!/boom!/plonk/#boo", "foo:bar:baz:/bang!/boom!/", "plonk/#boo" },
            { "foo:bar:baz:/bang!/boom!/plonk/#", "foo:bar:baz:/bang!/boom!/", "plonk/#" },
            { "foo:bar:baz:/bang!/boom!/plonk/", "foo:bar:baz:/bang!/boom!/", "plonk/" },
            { "foo:bar:baz:/bang!/boom!/plonk", "foo:bar:baz:/bang!/boom!/", "plonk" },
            { "foo:bar:baz:/bang!/boom!/", "foo:bar:baz:/bang!/boom!/", "" },

            { "foo:bar:/baz!/bang/../", "foo:bar:/baz!/", "" },
            { "foo:bar:/baz!/bang/..", "foo:bar:/baz!/", "" },
            { "foo:bar:/baz!/bang/./", "foo:bar:/baz!/", "bang/" },
            { "foo:bar:/baz!/bang/.", "foo:bar:/baz!/", "bang/" },

            { "foo:bar:/baz!/../bang/", "foo:bar:/baz!/", "bang/" },
            { "foo:bar:/baz!/./bang/", "foo:bar:/baz!/", "bang/" },
            { "foo:bar:/baz/../!/bang/", "foo:bar:/!/", "bang/" },
            { "foo:bar:/baz/..!/bang/", "foo:bar:/!/", "bang/" },
            { "foo:bar:/baz/./!/bang/", "foo:bar:/baz/!/", "bang/" },
            { "foo:bar:/baz/.!/bang/", "foo:bar:/baz/!/", "bang/" },
            { "foo:bar:/../baz/!/bang/", "foo:bar:/../baz/!/", "bang/" },
            { "foo:bar:/./baz/!/bang/", "foo:bar:/baz/!/", "bang/" },
            { "foo:bar://baz/!/bang/", "foo:bar://baz/!/", "bang/" }, // baz is authority!
            { "foo:bar://baz!/bang/", "foo:bar://baz!/", "bang/" }, // baz is authority!
            { "foo:bar:/baz!/bang/", "foo:bar:/baz!/", "bang/" },
            { "foo:bar:/!/bang/", "foo:bar:/!/", "bang/" },

            { "foo:bar:/baz!/../bang", "foo:bar:/baz!/", "bang" },
            { "foo:bar:/baz!/./bang", "foo:bar:/baz!/", "bang" },
            { "foo:bar:/baz/../!/bang", "foo:bar:/!/", "bang" },
            { "foo:bar:/baz/..!/bang", "foo:bar:/!/", "bang" },
            { "foo:bar:/baz/./!/bang", "foo:bar:/baz/!/", "bang" },
            { "foo:bar:/baz/.!/bang", "foo:bar:/baz/!/", "bang" },
            { "foo:bar:/../baz/!/bang", "foo:bar:/../baz/!/", "bang" },
            { "foo:bar:/./baz/!/bang", "foo:bar:/baz/!/", "bang" },
            { "foo:bar://baz/!/bang", "foo:bar://baz/!/", "bang" }, // baz is authority!
            { "foo:bar://baz!/bang", "foo:bar://baz!/", "bang" }, // baz is authority!
            { "foo:bar:/baz!/bang", "foo:bar:/baz!/", "bang" },
            { "foo:bar:/!/bang", "foo:bar:/!/", "bang" },

            { "foo:bar:/baz!/../", "foo:bar:/baz!/", "" },
            { "foo:bar:/baz!/..", "foo:bar:/baz!/", "" },
            { "foo:bar:/baz!/./", "foo:bar:/baz!/", "" },
            { "foo:bar:/baz!/.", "foo:bar:/baz!/", "" },
            { "foo:bar:/baz!//", "foo:bar:/baz!/", "" },
            { "foo:bar:/baz!/", "foo:bar:/baz!/", "" },

            { "foo:bar:/baz/!/", "foo:bar:/baz/!/", "" },
            { "foo:bar:/baz//!/", "foo:bar:/baz/!/", "" },
            { "foo:bar:/baz/./!/", "foo:bar:/baz/!/", "" },
            { "foo:bar:/baz/..!/", "foo:bar:/!/", "" },
            { "foo:bar:/baz/../!/", "foo:bar:/!/", "" },
            { "foo:bar:/baz!/bang//", "foo:bar:/baz!/", "bang/" },
            { "foo:bar:/baz!/bang/.", "foo:bar:/baz!/", "bang/" },
            { "foo:bar:/baz!/bang/./", "foo:bar:/baz!/", "bang/" },
            { "foo:bar:/baz!/bang/..", "foo:bar:/baz!/", "" },
            { "foo:bar:/baz!/bang/../", "foo:bar:/baz!/", "" },
        }) {
            final URI uri = new URI(params[0]);
            final URI mountPointUri = new URI(params[1]);
            final URI entryNameUri = new URI(params[2]);
            final Path path = new Path(uri, true);
            assertThat(path.getUri(),
                    equalTo(new URI(    mountPointUri.toString()
                                        + entryNameUri.toString())));
            assertThat(path.getMountPoint().getUri(), equalTo(mountPointUri));
            assertThat(path.getEntryName().getUri(), equalTo(entryNameUri));
            assertThat(path.toString(), equalTo(path.getUri().toString()));
            assertThat(path, equalTo(path));
            assertThat(path.hashCode(), equalTo(path.hashCode()));
        }
    }

    @Test
    @SuppressWarnings("ResultOfObjectAllocationIgnored")
    public void testConstructorWithMountPointAndEntryName()
    throws URISyntaxException {
        try {
            new Path(null, null);
            fail();
        } catch (NullPointerException expected) {
        }

        for (final String[] params : new String[][] {
            { "foo:bar:/baz!/", "foo:bar:/baz!/", "" },
            { "foo:bar:/baz!/a", "foo:bar:/baz!/", "a" },
            { "foo:bar:/baz!/a/", "foo:bar:/baz!/", "a/" },
        }) {
            final URI uri = new URI(params[0]);
            final URI mountPointUri = new URI(params[1]);
            final URI entryNameUri = new URI(params[2]);
            final MountPoint mountPoint = new MountPoint(mountPointUri);
            final EntryName entryName = new EntryName(entryNameUri);
            final Path path = new Path(mountPoint, entryName);

            assertThat(path.getUri(), equalTo(uri));
            assertThat(path.getMountPoint(), sameInstance(mountPoint));
            assertThat(path.getEntryName(), sameInstance(entryName));
            assertThat(path.toString(), equalTo(path.getUri().toString()));
            assertThat(path, equalTo(path));
            assertThat(path.hashCode(), equalTo(path.hashCode()));
        }
    }
}
