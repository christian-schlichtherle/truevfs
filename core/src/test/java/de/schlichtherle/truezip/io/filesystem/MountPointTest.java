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

import java.io.File;
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

    // TODO: Remove this!
    @Test
    public void testAssumptions() {
        assertThat(URI.create("foo:/bar/baz").resolve("."),
                equalTo(URI.create("foo:/bar/")));
        assertThat(URI.create("foo:/bar/baz/").resolve("."),
                equalTo(URI.create("foo:/bar/baz/")));
        assertThat(URI.create("foo:/bar/baz").resolve(".."),
                equalTo(URI.create("foo:/")));
        assertThat(URI.create("foo:/bar/baz/").resolve(".."),
                equalTo(URI.create("foo:/bar/")));
        assertThat(new File("c:/foo/bar").getParentFile().toURI(),
                equalTo(URI.create("file:/c:/foo")));
    }

    @Test
    @SuppressWarnings("ResultOfObjectAllocationIgnored")
    public void testConstructorWithNull() throws URISyntaxException {
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
            new MountPoint(null, null);
            fail();
        } catch (NullPointerException expected) {
        }

        final MountPoint parent = new MountPoint(URI.create("file:/foo"));
        try {
            new MountPoint(null, parent);
            fail();
        } catch (NullPointerException expected) {
        }
    }

    @Test
    @SuppressWarnings("ResultOfObjectAllocationIgnored")
    public void testConstructorWithPath() throws URISyntaxException {
        for (final String param : new String[] {
            "foo",
            "foo/bar",
            "foo/bar/",
            "/foo",
            "/foo/bar",
            "/foo/bar/",
            "//foo",
            "/../foo",
            "foo:bar:/baz!/bang",
        }) {
            final URI path = URI.create(param);
            try {
                MountPoint.create(path);
                fail(param);
            } catch (IllegalArgumentException expected) {
            }
        }

        for (final String param : new String[] {
            "foo:/bar",
            "foo:/bar/baz",
        }) {
            final URI path = new URI(param);
            final MountPoint mountPoint = new MountPoint(path);
            assertThat(mountPoint.getPath(), sameInstance(path));
            assertThat(mountPoint.getMember(), nullValue());
            assertThat(mountPoint.getParent(), nullValue());
            assertThat(mountPoint.toString(), equalTo(mountPoint.getPath().toString()));
            assertThat(mountPoint, equalTo(mountPoint));
            assertThat(mountPoint.hashCode(), equalTo(mountPoint.hashCode()));
        }

        for (final String[] params : new String[][] {
            { "foo:bar:/baz!/", "bar:/", "baz" },
            { "foo:bar:baz:/bang!/boom!/", "bar:baz:/bang!/", "boom" },
        }) {
            final URI path = new URI(params[0]);
            final URI parentPath = new URI(params[1]);
            final URI member = new URI(params[2]);
            final MountPoint mountPoint = new MountPoint(path);
            assertThat(mountPoint.getPath(), sameInstance(path));
            assertThat(mountPoint.getMember(), equalTo(member));
            assertThat(mountPoint.getParent().getPath(), equalTo(parentPath));
            assertThat(mountPoint.toString(), equalTo(mountPoint.getPath().toString()));
            assertThat(mountPoint, equalTo(mountPoint));
            assertThat(mountPoint, not(equalTo(mountPoint.getParent())));
            assertThat(mountPoint.hashCode(), equalTo(mountPoint.hashCode()));
        }
    }

    @Test
    @SuppressWarnings("ResultOfObjectAllocationIgnored")
    public void testConstructorWithPathAndParent() throws URISyntaxException {
        for (final String[] params : new String[][] {
            { "foo:/bar/", "foo:/baz/" },
            { "foo:bar:/baz!/", "bar:/bang/" },
            { "foo:bar:/baz!//", "bar:/baz/" },
            { "foo:bar:/baz!/.", "bar:/baz/" },
            { "foo:bar:/baz!/./", "bar:/baz/" },
            { "foo:bar:/baz!/..", "bar:/baz/" },
            { "foo:bar:/baz!/../", "bar:/baz/" },
            { "foo:bar:/baz!/a", "bar:/baz/" },
            { "foo:bar:/baz!/a/", "bar:/baz/" },
        }) {
            final URI path = new URI(params[0]);
            final URI parentPath = new URI(params[1]);
            final MountPoint parent = new MountPoint(parentPath);
            try {
                new MountPoint(path, parent);
                fail(params[0]);
            } catch (URISyntaxException expected) {
            }
        }

        for (final String[] params : new String[][] {
            { "foo:/bar/", "foo:/", "bar/" },
            { "foo:bar:/baz!/", "bar:/baz/", "" },
        }) {
            final URI path = new URI(params[0]);
            final URI parentPath = new URI(params[1]);
            final URI member = new URI(params[2]);
            final MountPoint parent = new MountPoint(parentPath);
            final MountPoint mountPoint = new MountPoint(path, parent);
            assertThat(mountPoint.getPath(), sameInstance(path));
            assertThat(mountPoint.getMember(), equalTo(member));
            assertThat(mountPoint.getParent(), sameInstance(parent));
            assertThat(mountPoint.toString(), equalTo(mountPoint.getPath().toString()));
            assertThat(mountPoint, equalTo(mountPoint));
            assertThat(mountPoint, not(equalTo(parent)));
            assertThat(mountPoint.hashCode(), equalTo(mountPoint.hashCode()));
        }
    }
}
