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
    public void testConstructorWithUri() throws URISyntaxException {
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
            "foo:/bar//",
            "foo:/bar/.",
            "foo:/bar/./",
            "foo:/bar/..",
            "foo:/bar/../",
            "foo:/bar#baz",
            "foo:bar:/baz!//",
            "foo:bar:/baz!/.",
            "foo:bar:/baz!/./",
            "foo:bar:/baz!/..",
            "foo:bar:/baz!/../",
            "foo:bar:/baz!/bang",
            "foo:bar:/baz!/#bang",
        }) {
            final URI uri = URI.create(param);
            try {
                MountPoint.create(uri);
                fail(param);
            } catch (IllegalArgumentException expected) {
            }
        }

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
            final URI uri = new URI(param);
            final MountPoint mountPoint = new MountPoint(uri);
            assertThat(mountPoint.getUri(), sameInstance(uri));
            assertThat(mountPoint.getPath(), nullValue());
            assertThat(mountPoint.toString(), equalTo(mountPoint.getUri().toString()));
            assertThat(mountPoint, equalTo(mountPoint));
            assertThat(mountPoint.hashCode(), equalTo(mountPoint.hashCode()));
        }

        for (final String[] params : new String[][] {
            { "foo:bar:baz:/bang/./!/boom/./!/", "foo", "bar:baz:/bang/!/boom/" },
            { "foo:bar:baz:/bang/.!/boom/.!/", "foo", "bar:baz:/bang/!/boom/" },
            { "foo:bar:baz:/bang/!/boom/!/", "foo", "bar:baz:/bang/!/boom/" },
            { "foo:bar:baz:/bang/!/boom/./!/", "foo", "bar:baz:/bang/!/boom/" },
            { "foo:bar:baz:/bang/!/boom/.!/", "foo", "bar:baz:/bang/!/boom/" },
            { "foo:bar:baz:/bang/!/boom/!/", "foo", "bar:baz:/bang/!/boom/" },
            { "foo:bar:baz:/bang!/boom/!/", "foo", "bar:baz:/bang!/boom/" },
            { "foo:bar:baz:/bang!/boom!/", "foo", "bar:baz:/bang!/boom" },
            { "foo:bar:/baz/!/", "foo", "bar:/baz/" },
            { "foo:bar:/baz!/", "foo", "bar:/baz" },
            { "foo:bar:/baz?bang!/", "foo", "bar:/baz?bang" },
        }) {
            final URI uri = new URI(params[0]);
            final String scheme = params[1];
            final URI pathUri = new URI(params[2]);
            final MountPoint mountPoint = new MountPoint(uri, true);
            assertThat(mountPoint.getUri(), equalTo(new URI(scheme + ":" + pathUri + "!/")));
            assertThat(mountPoint.getPath().getUri(), equalTo(pathUri));
            assertThat(mountPoint.toString(), equalTo(mountPoint.getUri().toString()));
            assertThat(mountPoint, equalTo(mountPoint));
            assertThat(mountPoint.hashCode(), equalTo(mountPoint.hashCode()));
        }
    }

    @Test
    @SuppressWarnings("ResultOfObjectAllocationIgnored")
    public void testConstructorWithSchemeAndPath() throws URISyntaxException {
        for (final String[] params : new String[][] {
            { "foo:/bar/", "foo:/baz/" },
            { "foo", "bar:baz:/!/" },
        }) {
            final String scheme = params[0];
            final URI pathUri = new URI(params[1]);
            final Path path = new Path(pathUri);
            try {
                new MountPoint(scheme, path);
                fail(params[0] + ":" + params[1] + "!/");
            } catch (URISyntaxException expected) {
            }
        }

        for (final String[] params : new String[][] {
            { "foo:bar:/baz!/", "foo", "bar:/baz" },
            { "foo:bar:/baz/!/", "foo", "bar:/baz/" },
        }) {
            final URI uri = new URI(params[0]);
            final String scheme = params[1];
            final URI pathUri = new URI(params[2]);
            final Path path = new Path(pathUri);
            final MountPoint mountPoint = new MountPoint(scheme, path);
            assertThat(mountPoint.getUri(), equalTo(uri));
            assertThat(mountPoint.getPath(), sameInstance(path));
            assertThat(mountPoint.toString(), equalTo(mountPoint.getUri().toString()));
            assertThat(mountPoint, equalTo(mountPoint));
            assertThat(mountPoint.hashCode(), equalTo(mountPoint.hashCode()));
        }
    }

    /*@Test
    public void testResolve() throws URISyntaxException {
        for (final String[] params : new String[][] {
            { "foo:bar:/baz/!/", "bang/", "baz/bang/" },
            { "foo:bar:/baz!/", "bang/", "baz/bang/" },
            { "foo:bar:/baz/!/", "bang", "baz/bang" },
            { "foo:bar:/baz!/", "bang", "baz/bang" },
            { "foo:bar:/!/", "baz/", "baz/" },
            { "foo:bar:/!/", "baz", "baz" },
        }) {
            final URI mountPointUri = new URI(params[0]);
            final URI entryNameUri = new URI(params[1]);
            final URI parentEntryNameUri = new URI(params[2]);
            final MountPoint mountPoint = new MountPoint(mountPointUri);
            final EntryName entryName = new EntryName(entryNameUri);
            final EntryName parentEntryName = mountPoint.resolve(entryName);
            assertThat(parentEntryName.getUri(), equalTo(parentEntryNameUri));
        }
    }*/
}
