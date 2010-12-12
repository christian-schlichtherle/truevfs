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
            { "foo:/bar/", "foo:/baz/" },
            { "foo", "bar:baz:/bang!/boom/" },
            { "foo", "bar:baz:/bang!/" },
            { "foo", "bar:/baz/" },
        }) {
            final String scheme = params[0];
            final Path path = new Path(URI.create(params[1]));
            try {
                new MountPoint(scheme, path);
                fail(params[0] + ":" + params[1] + "!/");
            } catch (URISyntaxException expected) {
            }
        }
    }

    @Test
    @SuppressWarnings("ResultOfObjectAllocationIgnored")
    public void testConstructorWithValidUri() throws URISyntaxException {
        for (final String param : new String[] {
            "foo:/bar/baz/",
            "foo:/bar/baz",
            "foo:/bar/",
            "foo:/bar",
            "foo:/",
            "foo:/bar/baz/?bang",
            "foo:/bar/baz?bang",
            "foo:/bar/?bang",
            "foo:/bar?bang",
            "foo:/?bang",
        }) {
            final URI uri = URI.create(param);
            final MountPoint mountPoint = new MountPoint(uri);
            assertThat(mountPoint.getUri(), sameInstance(uri));
            assertThat(mountPoint.getPath(), nullValue());
            assertThat(mountPoint.toString(), equalTo(mountPoint.getUri().toString()));
            assertThat(mountPoint, equalTo(MountPoint.create(URI.create(mountPoint.getUri().toString()))));
            assertThat(mountPoint.hashCode(), equalTo(MountPoint.create(URI.create(mountPoint.getUri().toString())).hashCode()));
        }

        for (final String[] params : new String[][] {
            { "foo:bar:baz:/bang!/boom?plonk!/", "foo", "bar:baz:/bang!/boom?plonk" },
            { "foo:bar:baz:/bang!/boom!/", "foo", "bar:baz:/bang!/boom" },
            { "foo:bar:/baz?bang!/", "foo", "bar:/baz?bang" },
            { "foo:bar:/baz!/", "foo", "bar:/baz" },
        }) {
            MountPoint mountPoint = new MountPoint(URI.create(params[0]), true);
            final String scheme = params[1];
            final Path path = new Path(URI.create(params[2]));
            testMountPoint(mountPoint, scheme, path);

            mountPoint = new MountPoint(scheme, path);
            testMountPoint(mountPoint, scheme, path);
        }
    }

    private void testMountPoint(final MountPoint mountPoint,
                                final String scheme,
                                final Path path) {
        assertThat(mountPoint.getUri(), equalTo(URI.create(scheme + ":" + path + "!/")));
        assertThat(mountPoint.getPath().getUri(), equalTo(path.getUri()));
        assertThat(mountPoint.toString(), equalTo(mountPoint.getUri().toString()));
        assertThat(mountPoint, equalTo(MountPoint.create(URI.create(mountPoint.getUri().toString()))));
        assertThat(mountPoint.hashCode(), equalTo(MountPoint.create(URI.create(mountPoint.getUri().toString())).hashCode()));
    }

    @Test
    public void testResolve() throws URISyntaxException {
        for (final String[] params : new String[][] {
            { "foo:bar:/baz?boom!/", "bang/?plonk", "baz/bang/?plonk" },
            { "foo:bar:/baz!/", "bang/?boom", "baz/bang/?boom" },
            { "foo:bar:/baz!/", "bang/", "baz/bang/" },
            { "foo:bar:/baz!/", "bang", "baz/bang" },
        }) {
            final MountPoint mountPoint = new MountPoint(URI.create(params[0]));
            final EntryName entryName = new EntryName(URI.create(params[1]));
            final EntryName parentEntryName = new EntryName(URI.create(params[2]));
            assertThat(mountPoint.resolve(entryName), equalTo(parentEntryName));
        }
    }
}
