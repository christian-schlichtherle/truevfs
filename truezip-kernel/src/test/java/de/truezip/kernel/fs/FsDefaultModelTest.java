/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.truezip.kernel.fs;

import de.truezip.kernel.fs.addr.FsEntryName;
import de.truezip.kernel.fs.addr.FsMountPoint;
import de.truezip.kernel.fs.addr.FsPath;
import java.net.URI;
import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;
import org.junit.Test;

/**
 * @author Christian Schlichtherle
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