/*
 * Copyright (C) 2011 Schlichtherle IT Services
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
package de.schlichtherle.truezip.fs;

import de.schlichtherle.truezip.fs.FsEntryName;
import de.schlichtherle.truezip.fs.FsMountPoint;
import de.schlichtherle.truezip.fs.FsTouchedListener;
import de.schlichtherle.truezip.fs.FsEvent;
import de.schlichtherle.truezip.fs.FsPath;
import de.schlichtherle.truezip.fs.FsModel;
import java.net.URI;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

/**
 * @author Christian Schlichtherle
 * @version $Id$
 */
public class FsModelTest {

    @Test
    @SuppressWarnings("ResultOfObjectAllocationIgnored")
    public void testConstructorWithNull() {
        try {
            new FsModel(null, null);
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
            final FsMountPoint mountPoint = FsMountPoint.create(URI.create(params[0]));
            final FsModel model = new FsModel(mountPoint);
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
            final FsMountPoint mountPoint = FsMountPoint.create(URI.create(params[0]));
            final FsMountPoint parentMountPoint = FsMountPoint.create(URI.create(params[1]));
            final FsModel parent = new FsModel(parentMountPoint);
            try {
                new FsModel(mountPoint, parent);
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
            final FsMountPoint mountPoint = FsMountPoint.create(URI.create(params[0]));
            final FsMountPoint parentMountPoint = FsMountPoint.create(URI.create(params[1]));
            final FsEntryName entryName = FsEntryName.create(URI.create(params[2]));
            final FsEntryName parentEntryName = FsEntryName.create(URI.create(params[3]));
            final FsPath path = FsPath.create(URI.create(params[4]));
            FsModel parent = newModel(parentMountPoint);
            FsModel model = new FsModel(mountPoint, parent);

            assertThat(model.getMountPoint(), sameInstance(mountPoint));
            assertThat(model.getParent(), sameInstance(parent));
            assertThat(model.getMountPoint().getPath().resolve(entryName).getEntryName(), equalTo(parentEntryName));
            assertThat(model.getMountPoint().resolve(entryName), equalTo(path));
            assertThat(model.isTouched(), is(false));
        }
    }

    private static FsModel newModel(final FsMountPoint mountPoint) {
        return new FsModel( mountPoint,
                                    null == mountPoint.getParent()
                                        ? null
                                        : newModel(mountPoint.getParent()));
    }

    @Test
    public void testAddRemoveFileSystemListeners() {
        final FsModel model = new FsModel(FsMountPoint.create(URI.create("foo:/")));

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
        final FsModel model = new FsModel(FsMountPoint.create(URI.create("foo:/")));
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
        final FsModel model;
        int changes;

        @SuppressWarnings("LeakingThisInConstructor")
        Listener(final FsModel model) {
            this.model = model;
            model.addFileSystemTouchedListener(this);
        }

        @Override
        public void touchedChanged(FsEvent event) {
            assertThat(event, notNullValue());
            assertThat(event.getSource(), sameInstance(model));
            changes++;
        }
    }
}
