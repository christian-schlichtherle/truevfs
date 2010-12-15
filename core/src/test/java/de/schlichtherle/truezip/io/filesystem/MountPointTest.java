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
public class MountPointTest {

    @Test
    @SuppressWarnings("ResultOfObjectAllocationIgnored")
    public void testConstructorWithInvalidUri() throws URISyntaxException {
        try {
            MountPoint.create(null);
            fail();
        } catch (NullPointerException expected) {
        }

        try {
            new MountPoint(null);
            fail();
        } catch (NullPointerException expected) {
        }

        try {
            MountPoint.create(null, false);
            fail();
        } catch (NullPointerException expected) {
        }

        try {
            new MountPoint(null, false);
            fail();
        } catch (NullPointerException expected) {
        }

        try {
            MountPoint.create(null, true);
            fail();
        } catch (NullPointerException expected) {
        }

        try {
            new MountPoint(null, true);
            fail();
        } catch (NullPointerException expected) {
        }

        try {
            MountPoint.create(null, null);
            fail();
        } catch (NullPointerException expected) {
        }

        try {
            new MountPoint(null, null);
            fail();
        } catch (NullPointerException expected) {
        }

        for (final String param : new String[] {
            "foo:bar:baz:/!/!/",
            "foo",
            "foo/bar",
            "foo/bar/",
            "/foo",
            "/foo/bar",
            "/foo/bar/",
            "//foo",
            "/../foo",
            "foo:/bar#baz",
            "foo:/bar/#baz",
            "foo:/bar",
            "foo:/bar?baz",
            "foo:/bar//",
            "foo:/bar/.",
            "foo:/bar/./",
            "foo:/bar/..",
            "foo:/bar/../",
            "foo:bar:/baz!//",
            "foo:bar:/baz!/.",
            "foo:bar:/baz!/./",
            "foo:bar:/baz!/..",
            "foo:bar:/baz!/../",
            "foo:bar:/baz!/bang",
            "foo:bar:/baz!/#bang",
            "foo:bar:baz:/bang!/!/",
        }) {
            final URI uri = URI.create(param);

            try {
                MountPoint.create(uri);
                fail(param);
            } catch (IllegalArgumentException expected) {
            }

            try {
                new MountPoint(uri);
                fail(param);
            } catch (URISyntaxException expected) {
            }
        }

        for (final String[] params : new String[][] {
            //{ "foo", "bar:baz:/bang!/boom" },
            { "foo", "bar:baz:/bang!/" },
            { "foo", "bar:/baz/" },
        }) {
            final Scheme scheme = Scheme.create(params[0]);
            final Path path = Path.create(URI.create(params[1]));
            try {
                new MountPoint(scheme, path);
                fail(params[0] + ":" + params[1] + "!/");
            } catch (URISyntaxException expected) {
            }
        }
    }

    @Test
    @SuppressWarnings("ResultOfObjectAllocationIgnored")
    public void testConstructorWithValidUri() {
        for (final String param : new String[] {
            "foo:/bär/bäz/",
            "foo:/bär/",
            "foo:/",
            "foo:/bär/bäz/?bäng",
            "foo:/bär/?bäng",
            "foo:/?bäng",
        }) {
            final URI uri = URI.create(param);
            final MountPoint mountPoint = MountPoint.create(uri);
            assertThat(mountPoint.getUri(), sameInstance(uri));
            assertThat(mountPoint.getPath(), nullValue());
            assertThat(mountPoint.toString(), equalTo(mountPoint.getUri().toString()));
            assertThat(mountPoint, equalTo(MountPoint.create(URI.create(mountPoint.getUri().toString()))));
            assertThat(mountPoint.hashCode(), equalTo(MountPoint.create(URI.create(mountPoint.getUri().toString())).hashCode()));
        }

        for (final String[] params : new String[][] {
            { "foo:bar:baz:/./bäng!/./bööm?plönk!/", "foo", "bar:baz:/bäng!/bööm?plönk" },
            { "foo:bar:baz:/./bang!/boom?plonk!/", "foo", "bar:baz:/bang!/boom?plonk" },
            { "foo:bar:baz:/bang!/./boom?plonk!/", "foo", "bar:baz:/bang!/boom?plonk" },
            { "foo:bar:baz:/bang!/boom?plonk!/", "foo", "bar:baz:/bang!/boom?plonk" },
            { "foo:bar:baz:/bang!/boom!/", "foo", "bar:baz:/bang!/boom" },
            { "foo:bar:/baz?bang!/", "foo", "bar:/baz?bang" },
            { "foo:bar:/baz!/", "foo", "bar:/baz" },
        }) {
            final MountPoint mountPoint = MountPoint.create(URI.create(params[0]), true);
            final Scheme scheme = Scheme.create(params[1]);
            final Path path = Path.create(URI.create(params[2]));

            assertThat(mountPoint.getScheme(), equalTo(scheme));
            assertThat(mountPoint.getPath(), equalTo(path));
            assertThat(mountPoint.toString(), equalTo(mountPoint.getUri().toString()));
            assertThat(MountPoint.create(URI.create(mountPoint.toString())), equalTo(mountPoint));
            assertThat(MountPoint.create(URI.create(mountPoint.getUri().getScheme() + ":" + mountPoint.getPath() + "!/")), equalTo(mountPoint));
            assertThat(MountPoint.create(mountPoint.getScheme(), mountPoint.getPath()), equalTo(mountPoint));
            assertThat(MountPoint.create(URI.create(mountPoint.getUri().toString())), equalTo(mountPoint));
            assertThat(MountPoint.create(URI.create(mountPoint.getUri().toString())).hashCode(), equalTo(mountPoint.hashCode()));
            assertThat(MountPoint.create(mountPoint.getScheme(), new Path(mountPoint.getParent(), mountPoint.resolveParent(FileSystemEntryName.ROOT))), equalTo(mountPoint));
            assertThat(MountPoint.create(mountPoint.resolveAbsolute(FileSystemEntryName.ROOT).getUri()), equalTo(mountPoint));
        }
    }

    @Test
    public void testResolve() {
        for (final String[] params : new String[][] {
            { "foo:bar:/bäz?bööm!/", "bäng?plönk", "bäz/bäng?plönk", "foo:bar:/bäz?bööm!/bäng?plönk" },
            { "foo:bar:/baz!/", "bang?boom", "baz/bang?boom", "foo:bar:/baz!/bang?boom" },
            { "foo:bar:/baz!/", "bang", "baz/bang", "foo:bar:/baz!/bang" },
            { "foo:/bar/?boom", "baz?plonk", null, "foo:/bar/baz?plonk" },
            { "foo:/bar/", "baz", null, "foo:/bar/baz" },
        }) {
            final MountPoint mountPoint = MountPoint.create(URI.create(params[0]));
            final FileSystemEntryName entryName = FileSystemEntryName.create(URI.create(params[1]));
            final FileSystemEntryName parentEntryName = null == params[2] ? null : FileSystemEntryName.create(URI.create(params[2]));
            final Path path = Path.create(URI.create(params[3]));
            if (null != parentEntryName)
                assertThat(mountPoint.resolveParent(entryName), equalTo(parentEntryName));
            assertThat(mountPoint.resolveAbsolute(entryName), equalTo(path));
            assertThat(mountPoint.resolveAbsolute(entryName).getUri().isAbsolute(), is(true));
        }
    }

    @Test
    public void testHierarchicalize() {
        for (final String[] params : new String[][] {
            { "foo:bar:baz:/x/bööm?plönk!/bäng?zönk!/", "baz:/x/bööm/bäng/?zönk" },
            { "foo:bar:baz:/boom?plonk!/bang?zonk!/", "baz:/boom/bang/?zonk" },
            { "foo:bar:baz:/boom!/bang!/", "baz:/boom/bang/" },
            { "foo:bar:/baz?boom!/", "bar:/baz/?boom" },
            { "foo:bar:/baz!/", "bar:/baz/" },
            { "foo:/bar/?boom", "foo:/bar/?boom" },
            { "foo:/bar/", "foo:/bar/" },
        }) {
            final MountPoint mountPoint = MountPoint.create(URI.create(params[0]));
            final URI flat = URI.create(params[1]);
            assertThat(mountPoint.hierarchicalize(), equalTo(flat));
        }
    }
}
