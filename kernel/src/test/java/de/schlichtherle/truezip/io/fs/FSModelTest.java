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
package de.schlichtherle.truezip.io.fs;

import java.net.URI;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

/**
 * @author Christian Schlichtherle
 * @version $Id$
 */
public class FSModelTest {

    @Test
    @SuppressWarnings("ResultOfObjectAllocationIgnored")
    public void testConstructorWithNull() {
        try {
            new FSModel1(null, null);
            fail();
        } catch (NullPointerException expected) {
        }
    }

    @Test
    @SuppressWarnings("ResultOfObjectAllocationIgnored")
    public void testConstructorWithMountPoint() {
        for (final String[] params : new String[][] {
            { "foo:/bar/" },
        }) {
            final FSMountPoint1 mountPoint = FSMountPoint1.create(URI.create(params[0]));
            final FSModel1 model = new FSModel1(mountPoint);
            assertThat(model.getMountPoint(), sameInstance(mountPoint));
            assertThat(model.getMountPoint().getPath(), nullValue());
            assertThat(model.getParent(), nullValue());
            assertThat(model.isTouched(), is(false));
        }
    }

    @Test
    @SuppressWarnings("ResultOfObjectAllocationIgnored")
    public void testConstructorWithMountPointAndParent() {
        for (final String[] params : new String[][] {
            { "foo:/bar/baz/", "foo:/bar/" },
            { "foo:/bar/", "foo:/baz/" },
        }) {
            final FSMountPoint1 mountPoint = FSMountPoint1.create(URI.create(params[0]));
            final FSMountPoint1 parentMountPoint = FSMountPoint1.create(URI.create(params[1]));
            final FSModel1 parent = new FSModel1(parentMountPoint);
            try {
                new FSModel1(mountPoint, parent);
                fail(params[0]);
            } catch (RuntimeException expected) {
            }
        }

        for (final String[] params : new String[][] {
            //{ "foo:bar:baz:/boom!/bang!/", "bar:baz:/boom!/", "plonk/", "bang/plonk/", "foo:bar:baz:/boom!/bang!/plonk/" },
            { "foo:bar:baz:/boom!/bang!/", "bar:baz:/boom!/", "plonk", "bang/plonk", "foo:bar:baz:/boom!/bang!/plonk" },
            //{ "foo:bar:/baz!/", "bar:/", "boom/", "baz/boom/", "foo:bar:/baz!/boom/" },
            { "foo:bar:/baz!/", "bar:/", "boom", "baz/boom", "foo:bar:/baz!/boom" },
        }) {
            final FSMountPoint1 mountPoint = FSMountPoint1.create(URI.create(params[0]));
            final FSMountPoint1 parentMountPoint = FSMountPoint1.create(URI.create(params[1]));
            final FSEntryName1 entryName = FSEntryName1.create(URI.create(params[2]));
            final FSEntryName1 parentEntryName = FSEntryName1.create(URI.create(params[3]));
            final FSPath1 path = FSPath1.create(URI.create(params[4]));
            FSModel1 parent = newModel(parentMountPoint);
            FSModel1 model = new FSModel1(mountPoint, parent);

            assertThat(model.getMountPoint(), sameInstance(mountPoint));
            assertThat(model.getParent(), sameInstance(parent));
            assertThat(model.getMountPoint().getPath().resolve(entryName).getEntryName(), equalTo(parentEntryName));
            assertThat(model.getMountPoint().resolve(entryName), equalTo(path));
            assertThat(model.isTouched(), is(false));
        }
    }

    private static FSModel1 newModel(final FSMountPoint1 mountPoint) {
        return new FSModel1( mountPoint,
                                    null == mountPoint.getParent()
                                        ? null
                                        : newModel(mountPoint.getParent()));
    }

    @Test
    public void testAddRemoveFileSystemListeners() {
        final FSModel1 model = new FSModel1(FSMountPoint1.create(URI.create("foo:/")));

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
        final FSModel1 model = new FSModel1(FSMountPoint1.create(URI.create("foo:/")));
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

    private static class Listener implements FsTouchedListener {
        final FSModel1 model;
        int changes;

        @SuppressWarnings("LeakingThisInConstructor")
        Listener(final FSModel1 model) {
            this.model = model;
            model.addFileSystemTouchedListener(this);
        }

        @Override
        public void touchedChanged(FSEvent1 event) {
            assertThat(event, notNullValue());
            assertThat(event.getSource(), sameInstance(model));
            changes++;
        }
    }
}
