/*
 * Copyright © 2005 - 2021 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package global.namespace.truevfs.it.base;

import global.namespace.truevfs.kernel.api.FsModel;
import global.namespace.truevfs.kernel.api.FsMountPoint;
import global.namespace.truevfs.kernel.api.FsNodeName;
import global.namespace.truevfs.kernel.api.FsNodePath;
import org.junit.Test;

import java.net.URI;
import java.util.Optional;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;

/**
 * @author Christian Schlichtherle
 */
@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
public final class FsModelTest {

    protected FsModel newModel(FsMountPoint mountPoint, Optional<? extends FsModel> parent) {
        return new FsTestModel(mountPoint, parent);
    }

    private FsModel newModel(FsMountPoint mountPoint) {
        return newModel(mountPoint, mountPoint.getParent().map(this::newModel));
    }

    @Test
    public void testConstructorWithNull() {
        try {
            newModel(null, null);
            fail();
        } catch (NullPointerException expected) {
        }
    }

    @Test
    public void testConstructorWithMountPoint() {
        for (final String[] params : new String[][] {
            { "foo:/bar/" },
        }) {
            final FsMountPoint mountPoint = FsMountPoint.create(URI.create(params[0]));
            final FsModel model = newModel(mountPoint, Optional.empty());
            assertThat(model.getMountPoint(), sameInstance(mountPoint));
            assertFalse(model.getMountPoint().getPath().isPresent());
            assertFalse(model.getParent().isPresent());
            assertThat(model.isMounted(), is(false));
        }
    }

    @Test
    public void testConstructorWithMountPointAndParent() {
        for (final String[] params : new String[][] {
            { "foo:/bar/baz/", "foo:/bar/" },
            { "foo:/bar/", "foo:/baz/" },
        }) {
            final FsMountPoint mountPoint = FsMountPoint.create(URI.create(params[0]));
            final FsMountPoint parentMountPoint = FsMountPoint.create(URI.create(params[1]));
            final FsModel parent = newModel(parentMountPoint, Optional.empty());
            try {
                newModel(mountPoint, Optional.of(parent));
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
            final FsNodeName entryName = FsNodeName.create(URI.create(params[2]));
            final FsNodeName parentEntryName = FsNodeName.create(URI.create(params[3]));
            final FsNodePath path = FsNodePath.create(URI.create(params[4]));
            FsModel parent = newModel(parentMountPoint);
            FsModel model = newModel(mountPoint, Optional.of(parent));

            assertThat(model.getMountPoint(), sameInstance(mountPoint));
            assertThat(model.getParent().get(), sameInstance(parent));
            assertThat(model.getMountPoint().getPath().get().resolve(entryName).getNodeName(), equalTo(parentEntryName));
            assertThat(model.getMountPoint().resolve(entryName), equalTo(path));
            assertThat(model.isMounted(), is(false));
        }
    }
}
