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

import de.schlichtherle.truezip.io.entry.Entry;
import java.net.URI;
import org.junit.Test;

import static de.schlichtherle.truezip.io.entry.Entry.ROOT;
import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

/**
 * @author Christian Schlichtherle
 * @version $Id$
 */
public class FileSystemModelTest {

    @Test
    @SuppressWarnings("ResultOfObjectAllocationIgnored")
    public void testConstructor() {
        try {
            new FileSystemModel(null, null, null);
            fail();
        } catch (NullPointerException expected) {
        }
    }

    @Test
    @SuppressWarnings("ResultOfObjectAllocationIgnored")
    public void testMountPointConstructor() {
        for (final String uri : new String[] {
            "foo:bar",
            "/bar",
        }) {
            final URI mountPoint = URI.create(uri);
            try {
                new FileSystemModel(mountPoint, null, null);
                fail();
            } catch (RuntimeException expected) {
            }
        }

        final URI expectedMountPoint = URI.create("foo:/bar/");
        for (final String uri : new String[] {
            "foo:/bar",
            "foo:/bar/",
            "foo:/bar//",
        }) {
            final URI mountPoint = URI.create(uri);
            final FileSystemModel model = new FileSystemModel(mountPoint, null, null);
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
    public void testMountPointAndParentConstructor() {
        for (final String[] uri : new String[][] {
            { "foo:bar", "foo:/" },
            { "/bar", "/" },
            { "foo:/bar", "foo:/baz" },
            { "foo:/bar/", "foo:/baz/" },
            { "foo:bar:/a!/b", "bar:/a" },
        }) {
            final URI mountPoint = URI.create(uri[0]);
            final URI parentMountPoint = URI.create(uri[1]);
            try {
                new FileSystemModel(mountPoint, new FileSystemModel(parentMountPoint, null, null), null);
                fail();
            } catch (RuntimeException expected) {
            }
        }

        for (final String[] uri : new String[][] {
            { "foo:/bar/", "foo:/bar", "foo:/", "bar", "" },
            { "foo:/bar/", "foo:/bar/", "foo:/", "bar", "" },
            { "foo:/bar/", "foo:/bar//", "foo:/", "bar", "" },
            { "foo:/bar/", "foo:/bar//", "foo:/", "bar/baz", "baz" },
            { "foo:/bar/", "foo:/bar//", "foo:/", "bar/baz/", "baz/" },
        }) {
            final URI expectedMountPoint = URI.create(uri[0]);
            final URI mountPoint = URI.create(uri[1]);
            final URI parentMountPoint = URI.create(uri[2]);
            final String parentPath = uri[3];
            final String path = uri[4];

            final FileSystemModel model = new FileSystemModel(mountPoint, new FileSystemModel(parentMountPoint, null, null), null);
            assertThat(model.getMountPoint(), equalTo(expectedMountPoint));
            assertThat(model.getParent(), notNullValue());
            assertThat(model.getParent().getMountPoint(), equalTo(parentMountPoint));
            assertThat(model.parentPath(path), equalTo(parentPath));
            assertThat(model.isTouched(), is(false));
        }
    }

    @Test
    @SuppressWarnings("ResultOfObjectAllocationIgnored")
    public void testMountPointAndFactoryConstructor() {
        for (final String[] uri : new String[][] {
            { "/bar" },
            { "foo:bar" },
            { "foo:bar:a!/b" },
        }) {
            final URI mountPoint = URI.create(uri[0]);
            try {
                new FileSystemModel(mountPoint, null, new DummyFactory());
                fail();
            } catch (RuntimeException expected) {
            }
        }

        for (final String[] uri : new String[][] {
            { "foo:/bar/", "foo:/bar", null, null, null },
            { "foo:bar:/a!/b/", "foo:bar:/a!/b", "bar:/a/", "b", "" },
            { "foo:bar:/a!/b/", "foo:bar:/a!/b", "bar:/a/", "b/c", "c" },
        }) {
            final URI expectedMountPoint = URI.create(uri[0]);
            final URI mountPoint = URI.create(uri[1]);
            final URI parentMountPoint = null == uri[2] ? null : URI.create(uri[2]);
            final String parentPath = uri[3];
            final String path = uri[4];
            final FileSystemModel model = new FileSystemModel(mountPoint, null, new DummyFactory());
            assertThat(model.getMountPoint(), equalTo(expectedMountPoint));
            if (null != parentMountPoint) {
                assertThat(model.getParent(), notNullValue());
                assertThat(model.getParent().getMountPoint(), equalTo(parentMountPoint));
                assertThat(model.parentPath(path), equalTo(parentPath));
            } else {
                assertThat(model.getParent(), nullValue());
            }
            assertThat(model.isTouched(), is(false));
        }
    }

    private static class DummyFactory
    implements FileSystemFactory<FileSystemModel, Entry> {

        @Override
        public FileSystemModel newModel(URI mountPoint, FileSystemModel parent) {
            return new FileSystemModel(mountPoint, null, this);
        }

        @Override
        public FileSystemController<Entry> newController(FileSystemModel model, ComponentFileSystemController<?> parent) {
            throw new UnsupportedOperationException();
        }
    }

    @Test
    public void testAddRemoveFileSystemListeners() {
        final FileSystemModel model = new FileSystemModel(URI.create("foo:/bar"), null, null);

        try {
            model.addFileSystemListener(null);
        } catch (NullPointerException expected) {
        }
        assertThat(model.getFileSystemListeners(), notNullValue());
        assertThat(model.getFileSystemListeners().size(), is(0));

        final Listener listener1 = new Listener(model);
        model.addFileSystemListener(listener1);
        assertThat(model.getFileSystemListeners().size(), is(1));

        final Listener listener2 = new Listener(model);
        model.addFileSystemListener(listener2);
        assertThat(model.getFileSystemListeners().size(), is(2));

        model.getFileSystemListeners().clear();
        assertThat(model.getFileSystemListeners().size(), is(2));

        try {
            model.removeFileSystemListener(null);
        } catch (NullPointerException expected) {
        }
        assertThat(model.getFileSystemListeners().size(), is(2));

        model.removeFileSystemListener(listener1);
        model.removeFileSystemListener(listener1);
        assertThat(model.getFileSystemListeners().size(), is(1));

        model.removeFileSystemListener(listener2);
        model.removeFileSystemListener(listener2);
        assertThat(model.getFileSystemListeners().size(), is(0));
    }

    @Test
    public void testNotifyFileSystemListeners() {
        final FileSystemModel model = new FileSystemModel(URI.create("foo:/bar"), null, null);
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

    private static class Listener implements FileSystemListener {
        final FileSystemModel model;
        int changes;

        @SuppressWarnings("LeakingThisInConstructor")
        Listener(final FileSystemModel model) {
            this.model = model;
            model.addFileSystemListener(this);
        }

        @Override
        public void touchChanged(FileSystemEvent event) {
            assertThat(event, notNullValue());
            assertThat(event.getSource(), sameInstance(model));
            changes++;
        }
    }
}
