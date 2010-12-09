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
    public void testConstructorWithNull() throws URISyntaxException {
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
            new Path(null, null);
            fail();
        } catch (NullPointerException expected) {
        }

        final Path parent = new Path(URI.create("file:/"));
        try {
            new Path(null, parent);
            fail();
        } catch (NullPointerException expected) {
        }
    }

    @Test
    @SuppressWarnings("ResultOfObjectAllocationIgnored")
    public void testConstructorWithPath() throws URISyntaxException {
        for (final String param : new String[] {
            "foo:bar",
            "foo:bar:",
            "foo:bar:/",
            "foo:bar:/baz",
            "foo:bar:/baz!",
            "foo:bar:/baz/",
            "foo:bar:baz:/bang",
            "foo:bar:baz:/bang!",
            "foo:bar:baz:/bang/",
            "foo:bar:baz:/bang!/",
        }) {
            final URI name = URI.create(param);
            try {
                Path.create(name);
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
            final URI name = new URI(param);
            final Path path = new Path(name);
            assertThat(path.getPathName(), sameInstance(name));
            assertThat(path.getEntryName(), nullValue());
            assertThat(path.getMountPoint(), nullValue());
            assertThat(path.toString(), equalTo(path.getPathName().toString()));
            assertThat(path, equalTo(path));
            assertThat(path.hashCode(), equalTo(path.hashCode()));
        }

        for (final String[] params : new String[][] {
            { "foo:bar:baz:/bang!/boom!/plonk/#boo", "bar:baz:/bang!/boom", "plonk/#boo" },
            { "foo:bar:baz:/bang!/boom!/plonk/#", "bar:baz:/bang!/boom", "plonk/#" },
            { "foo:bar:baz:/bang!/boom!/plonk/", "bar:baz:/bang!/boom", "plonk/" },
            { "foo:bar:baz:/bang!/boom!/plonk", "bar:baz:/bang!/boom", "plonk" },
            { "foo:bar:baz:/bang!/boom!/", "bar:baz:/bang!/boom", "" },

            { "foo:bar:/baz!/bang/../", "bar:/baz", "bang/../" },
            { "foo:bar:/baz!/bang/..", "bar:/baz", "bang/.." },
            { "foo:bar:/baz!/bang/./", "bar:/baz", "bang/./" },
            { "foo:bar:/baz!/bang/.", "bar:/baz", "bang/." },

            { "foo:bar:/baz!/../bang/", "bar:/baz", "../bang/" },
            { "foo:bar:/baz!/./bang/", "bar:/baz", "./bang/" },
            { "foo:bar:/baz/../!/bang/", "bar:/baz/../", "bang/" },
            { "foo:bar:/baz/..!/bang/", "bar:/baz/..", "bang/" },
            { "foo:bar:/baz/./!/bang/", "bar:/baz/./", "bang/" },
            { "foo:bar:/baz/.!/bang/", "bar:/baz/.", "bang/" },
            { "foo:bar:/../baz/!/bang/", "bar:/../baz/", "bang/" },
            { "foo:bar:/./baz/!/bang/", "bar:/./baz/", "bang/" },
            { "foo:bar://baz/!/bang/", "bar://baz/", "bang/" }, // baz is authority!
            { "foo:bar://baz!/bang/", "bar://baz", "bang/" }, // baz is authority!
            { "foo:bar:/baz!/bang/", "bar:/baz", "bang/" },
            { "foo:bar:/!/bang/", "bar:/", "bang/" },

            { "foo:bar:/baz!/../bang", "bar:/baz", "../bang" },
            { "foo:bar:/baz!/./bang", "bar:/baz", "./bang" },
            { "foo:bar:/baz/../!/bang", "bar:/baz/../", "bang" },
            { "foo:bar:/baz/..!/bang", "bar:/baz/..", "bang" },
            { "foo:bar:/baz/./!/bang", "bar:/baz/./", "bang" },
            { "foo:bar:/baz/.!/bang", "bar:/baz/.", "bang" },
            { "foo:bar:/../baz/!/bang", "bar:/../baz/", "bang" },
            { "foo:bar:/./baz/!/bang", "bar:/./baz/", "bang" },
            { "foo:bar://baz/!/bang", "bar://baz/", "bang" }, // baz is authority!
            { "foo:bar://baz!/bang", "bar://baz", "bang" }, // baz is authority!
            { "foo:bar:/baz!/bang", "bar:/baz", "bang" },
            { "foo:bar:/!/bang", "bar:/", "bang" },

            { "foo:bar:/baz!/../", "bar:/baz", "../" },
            { "foo:bar:/baz!/..", "bar:/baz", ".." },
            { "foo:bar:/baz!/./", "bar:/baz", "./" },
            { "foo:bar:/baz!/.", "bar:/baz", "." },
            { "foo:bar:/baz!//", "bar:/baz", "/" },
            { "foo:bar:/baz!/", "bar:/baz", "" },

            { "foo:bar:/baz/!/", "bar:/baz/", "" },
            { "foo:bar:/baz//!/", "bar:/baz//", "" },
            { "foo:bar:/baz/./!/", "bar:/baz/./", "" },
            { "foo:bar:/baz/..!/", "bar:/baz/..", "" },
            { "foo:bar:/baz/../!/", "bar:/baz/../", "" },
            { "foo:bar:/baz!/bang//", "bar:/baz", "bang//" },
            { "foo:bar:/baz!/bang/.", "bar:/baz", "bang/." },
            { "foo:bar:/baz!/bang/./", "bar:/baz", "bang/./" },
            { "foo:bar:/baz!/bang/..", "bar:/baz", "bang/.." },
            { "foo:bar:/baz!/bang/../", "bar:/baz", "bang/../" },
        }) {
            final URI name = new URI(params[0]);
            final URI mountPointName = new URI(params[1]);
            final URI entry = new URI(params[2]);
            final Path path = new Path(name);
            assertThat(path.getPathName(), sameInstance(name));
            assertThat(path.getEntryName(), equalTo(entry));
            assertThat(path.getMountPoint().getPathName(), equalTo(mountPointName));
            assertThat(path.toString(), equalTo(path.getPathName().toString()));
            assertThat(path, equalTo(path));
            assertThat(path, not(equalTo(path.getMountPoint())));
            assertThat(path.hashCode(), equalTo(path.hashCode()));
        }
    }

    @Test
    @SuppressWarnings("ResultOfObjectAllocationIgnored")
    public void testConstructorWithPathAndParent() throws URISyntaxException {
        for (final String[] params : new String[][] {
            /*{ "foo:bar", "foo:/" },
            { "foo:/bar", "foo:/ba" },
            { "foo:/bar", "foo:/bar" },
            { "foo:/bar", "foo:/baz/" },
            { "foo:/bar/", "foo:/bar" },
            { "foo:/bar/", "foo:/bar/" },
            { "foo:/bar/", "foo:/baz/" },*/
            { "jar:file:/lib.jar", "file:/" },
            { "jar:file:/lib.jar!", "file:/" },
            { "jar:file:/lib.jar!//", "file:/" },
            { "jar:file:/lib.jar!/entry", "file:/" },
        }) {
            final URI name = new URI(params[0]);
            final URI mountPointName = new URI(params[1]);
            final Path mountPoint = new Path(mountPointName);
            try {
                new Path(name, mountPoint);
                fail(params[0]);
            } catch (URISyntaxException expected) {
            }
        }

        for (final String[] params : new String[][] {
            /*{ "foo:/bar", "foo:/", "bar" },
            { "foo:/bar/", "foo:/", "bar/" },
            { "foo:/bar/baz", "foo:/bar", "baz" },
            { "foo:/bar/baz/", "foo:/bar", "baz/" },
            { "foo:/bar/baz", "foo:/bar/", "baz" },
            { "foo:/bar/baz/", "foo:/bar/", "baz/" },*/
            { "foo:bar:/baz!/", "bar:/baz", "" },
            { "foo:bar:/baz!/a", "bar:/baz", "a" },
            { "foo:bar:/baz!/a/", "bar:/baz", "a/" },
        }) {
            final URI pathName = new URI(params[0]);
            final URI mountPointName = new URI(params[1]);
            final URI entryName = new URI(params[2]);
            final Path mountPoint = new Path(mountPointName);
            final Path path = new Path(pathName, mountPoint);
            assertThat(path.getPathName(), sameInstance(pathName));
            assertThat(path.getEntryName(), equalTo(entryName));
            assertThat(path.getMountPoint(), sameInstance(mountPoint));
            assertThat(path.toString(), equalTo(path.getPathName().toString()));
            assertThat(path, equalTo(path));
            assertThat(path, not(equalTo(mountPoint)));
            assertThat(path.hashCode(), equalTo(path.hashCode()));
        }
    }
}
