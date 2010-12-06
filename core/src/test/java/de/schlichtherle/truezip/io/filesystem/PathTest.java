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
import org.junit.Test;

import static de.schlichtherle.truezip.io.filesystem.FileSystemEntry.ROOT;
import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

/**
 * @author Christian Schlichtherle
 * @version $Id$
 */
public class PathTest {

    @Test
    @SuppressWarnings("ResultOfObjectAllocationIgnored")
    public void testConstructorWithNull() {
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
    public void testConstructorWithUri() {
        for (final String param : new String[] {
            "foo//",
            "foo/.",
            "foo/./",
            "foo/..",
            "foo/../",
            "/./foo",
            "foo:/bar//",
            "foo:/bar/.",
            "foo:/bar/./",
            "foo:/bar/..",
            "foo:/bar/../",
            "foo:bar",
            "foo:bar:/",
        }) {
            final URI uri = URI.create(param);
            try {
                new Path(uri);
                fail(param);
            } catch (IllegalArgumentException expected) {
            }
        }

        for (final String param : new String[] {
            "foo",
            "foo/bar",
            "foo/bar/",
            "/foo",
            "/foo/bar",
            "/foo/bar/",
            "//foo",
            "/../foo",
            "foo:/bar",
            "foo:/bar/",
        }) {
            final URI uri = URI.create(param);
            final Path path = new Path(uri);
            assertThat(path.getUri(), sameInstance(uri));
            assertThat(path.getParent(), nullValue());
            try {
                path.parentPath(ROOT);
                fail(param);
            } catch (NullPointerException expected) {
            }
        }
    }

    @Test
    @SuppressWarnings("ResultOfObjectAllocationIgnored")
    public void testConstructorWithUriAndParent() {
        for (final String[] params : new String[][] {
            { "foo:bar", "foo:/" },
            { "foo:/bar", "foo:/baz/" },
            { "foo:/bar/", "foo:/baz/" },
            { "jar:file:/lib.jar", "file:/" },
            { "jar:file:/lib.jar!", "file:/" },
            { "jar:file:/lib.jar!//", "file:/" },
            { "jar:file:/lib.jar!/entry", "file:/" },
        }) {
            final URI uri = URI.create(params[0]);
            final URI parentURI = URI.create(params[1]);
            final Path parent = new Path(parentURI);
            try {
                new Path(uri, parent);
                fail(params[0]);
            } catch (IllegalArgumentException expected) {
            }
        }

        for (final String[] params : new String[][] {
            { "foo:bar:/baz/a!/", "bar:/", "", "baz/a/" },
            { "foo:bar:/baz/a!/", "bar:/baz", "", "/a/" },
            { "foo:bar:/baz/a!/", "bar:/baz", "b", "/a/b" },
            { "foo:bar:/baz/a!/", "bar:/baz", "b/", "/a/b/" },
            { "foo:bar:/baz/a!/", "bar:/baz/", "", "a/" },
            { "foo:bar:/baz/a!/", "bar:/baz/", "b", "a/b" },
            { "foo:bar:/baz/a!/", "bar:/baz/", "b/", "a/b/" },
            { "bar:/baz/a/", "bar:/baz/", "", "a/" },
            { "bar:/baz/a/", "bar:/baz/", "b", "a/b" },
            { "bar:/baz/a/", "bar:/baz/", "b/", "a/b/" },
        }) {
            final URI uri = URI.create(params[0]);
            final URI parentUri = URI.create(params[1]);
            final String name = params[2];
            final String parentName = params[3];
            final Path parent = new Path(parentUri);
            final Path path = new Path(uri, parent);

            assertThat(path.getUri(), sameInstance(uri));
            assertThat(path.getParent(), sameInstance(parent));
            assertThat(path.parentPath(name).toString(), equalTo(parentName));
        }
    }
}
