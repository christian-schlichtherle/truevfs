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

import java.net.URI;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

/**
 * @author Christian Schlichtherle
 * @version $Id$
 */
public class FsDefaultModelTest {

    @Test
    @SuppressWarnings("ResultOfObjectAllocationIgnored")
    public void testConstructorWithNull() {
        try {
            new FsDefaultModel(null, null);
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
            final FsModel model = new FsDefaultModel(mountPoint, null);
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
            final FsModel parent = new FsDefaultModel(parentMountPoint, null);
            try {
                new FsDefaultModel(mountPoint, parent);
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
            FsModel model = new FsDefaultModel(mountPoint, parent);

            assertThat(model.getMountPoint(), sameInstance(mountPoint));
            assertThat(model.getParent(), sameInstance(parent));
            assertThat(model.getMountPoint().getPath().resolve(entryName).getEntryName(), equalTo(parentEntryName));
            assertThat(model.getMountPoint().resolve(entryName), equalTo(path));
            assertThat(model.isTouched(), is(false));
        }
    }

    private static FsModel newModel(final FsMountPoint mountPoint) {
        return new FsDefaultModel(  mountPoint,
                                    null == mountPoint.getParent()
                                        ? null
                                        : newModel(mountPoint.getParent()));
    }
}
