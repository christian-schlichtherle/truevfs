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
public class FileSystemModelTest {

    @Test
    @SuppressWarnings("ResultOfObjectAllocationIgnored")
    public void testConstructorWithNull() {
        try {
            new FileSystemModel(null, null);
            fail();
        } catch (NullPointerException expected) {
        }
    }

    @Test
    @SuppressWarnings("ResultOfObjectAllocationIgnored")
    public void testConstructorWithMountPoint() {
        for (final String uri : new String[] {
            "foo",
            "foo:bar",
            "foo:bar:/",
            "/bar",
        }) {
            final URI mountPoint = URI.create(uri);
            try {
                new FileSystemModel(mountPoint, null);
                fail();
            } catch (RuntimeException expected) {
            }
        }

        for (final String[] params : new String[][] {
            { "foo:/bar/", "foo:/bar/" },
            { "foo:/bar/", "foo:/bar/" },
            { "foo:/bar/", "foo:/bar//" },
        }) {
            final URI expectedMountPoint = URI.create(params[0]);
            final URI mountPoint = URI.create(params[1]);
            final FileSystemModel model = new FileSystemModel(mountPoint, null);
            assertThat(model.getMountPoint(), equalTo(expectedMountPoint));
            assertThat(model.getParent(), nullValue());
            try {
                model.parentPath(ROOT);
                fail();
            } catch (RuntimeException expected) {
            }
            assertThat(model.isTouched(), is(false));
        }
    }

    @Test
    @SuppressWarnings("ResultOfObjectAllocationIgnored")
    public void testConstructorWithMountPointAndParent() {
        for (final String[] params : new String[][] {
            { "foo:bar", "foo:/" },
            { "foo:/bar", "foo:/baz/" },
            { "foo:/bar/", "foo:/baz/" },
            { "foo:/baz/bang", "foo:/baz/" },
            { "jar:file:/lib.jar", "file:/" },
            { "jar:file:/lib.jar!", "file:/" },
            { "jar:file:/lib.jar!/entry", "file:/" },
        }) {
            final URI mountPoint = URI.create(params[0]);
            final URI parentMountPoint = URI.create(params[1]);
            final FileSystemModel parent = new FileSystemModel(parentMountPoint, null);
            try {
                new FileSystemModel(mountPoint, parent);
                fail();
            } catch (RuntimeException expected) {
            }
        }

        for (final String[] params : new String[][] {
            { "foo:bar:/baz/a!/", "foo:bar:/baz/a!/", "bar:/baz/", "bar:/baz/", "a", "" },
            { "foo:bar:/baz/a!/", "foo:bar:/baz/a!/", "bar:/baz/", "bar:/baz/", "a/b", "b" },
            { "foo:bar:/baz/a!/", "foo:bar:/baz/a!/", "bar:/baz/", "bar:/baz/", "a/b/", "b/" },
            { "foo:bar:/baz/a!/", "foo:bar:/baz/a!/", "bar:/baz/", "bar:/baz//", "a/b//", "b//" },
            { "bar:/baz/a/", "bar:/baz/a/", "bar:/baz/", "bar:/baz/", "a", "" },
            { "bar:/baz/a/", "bar:/baz/a/", "bar:/baz/", "bar:/baz/", "a/b", "b" },
            { "bar:/baz/a/", "bar:/baz//a/", "bar:/baz/", "bar:/baz/", "a/b/", "b/" },
            { "bar:/baz/a/", "bar:/baz//a//", "bar:/baz/", "bar:/baz//", "a/b//", "b//" },
        }) {
            final URI expectedMountPoint = URI.create(params[0]);
            final URI mountPoint = URI.create(params[1]);
            final URI expectedParentMountPoint = URI.create(params[2]);
            final URI parentMountPoint = URI.create(params[3]);
            final String parentPath = params[4];
            final String path = params[5];
            final FileSystemModel parent = new FileSystemModel(parentMountPoint, null);
            final FileSystemModel model = new FileSystemModel(mountPoint, parent);

            assertThat(parent.getMountPoint(), equalTo(expectedParentMountPoint));
            assertThat(model.getMountPoint(), equalTo(expectedMountPoint));
            assertThat(model.getParent(), sameInstance(parent));
            assertThat(model.parentPath(path), equalTo(parentPath));
            assertThat(model.isTouched(), is(false));
        }
    }

    @Test
    public void testAddRemoveFileSystemListeners() {
        final FileSystemModel model = new FileSystemModel(URI.create("foo:/bar/"), null);

        try {
            model.addFileSystemTouchedListener(null);
        } catch (NullPointerException expected) {
        }
        assertThat(model.getFileSystemTouchedListeners(), notNullValue());
        assertThat(model.getFileSystemTouchedListeners().size(), is(0));

        final Listener listener1 = new Listener(model);
        model.addFileSystemTouchedListener(listener1);
        assertThat(model.getFileSystemTouchedListeners().size(), is(1));

        final Listener listener2 = new Listener(model);
        model.addFileSystemTouchedListener(listener2);
        assertThat(model.getFileSystemTouchedListeners().size(), is(2));

        model.getFileSystemTouchedListeners().clear();
        assertThat(model.getFileSystemTouchedListeners().size(), is(2));

        try {
            model.removeFileSystemTouchedListener(null);
        } catch (NullPointerException expected) {
        }
        assertThat(model.getFileSystemTouchedListeners().size(), is(2));

        model.removeFileSystemTouchedListener(listener1);
        model.removeFileSystemTouchedListener(listener1);
        assertThat(model.getFileSystemTouchedListeners().size(), is(1));

        model.removeFileSystemTouchedListener(listener2);
        model.removeFileSystemTouchedListener(listener2);
        assertThat(model.getFileSystemTouchedListeners().size(), is(0));
    }

    @Test
    public void testNotifyFileSystemListeners() {
        final FileSystemModel model = new FileSystemModel(URI.create("foo:/bar/"), null);
        final Listener listener1 = new Listener(model);
        final Listener listener2 = new Listener(model);

        model.setTouched(false);
        assertThat(listener1.changes, is(0));
        assertThat(listener2.changes, is(0));

        model.setTouched(true);
        assertThat(listener1.changes, is(1));
        assertThat(listener2.changes, is(1));

        model.setTouched(true);
        assertThat(listener1.changes, is(1));
        assertThat(listener2.changes, is(1));

        model.setTouched(false);
        assertThat(listener1.changes, is(2));
        assertThat(listener2.changes, is(2));
    }

    private static class Listener implements FileSystemTouchedListener {
        final FileSystemModel model;
        int changes;

        @SuppressWarnings("LeakingThisInConstructor")
        Listener(final FileSystemModel model) {
            this.model = model;
            model.addFileSystemTouchedListener(this);
        }

        @Override
        public void touchedChanged(FileSystemEvent event) {
            assertThat(event, notNullValue());
            assertThat(event.getSource(), sameInstance(model));
            changes++;
        }
    }
}
