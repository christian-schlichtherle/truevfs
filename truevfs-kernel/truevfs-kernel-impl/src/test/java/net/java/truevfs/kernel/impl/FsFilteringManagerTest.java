/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.kernel.impl;

import java.net.URI;
import java.util.HashSet;
import java.util.Set;
import net.java.truecommons.shed.Link.Type;
import static net.java.truecommons.shed.Link.Type.STRONG;
import net.java.truevfs.kernel.driver.mock.MockDriverMapContainer;
import net.java.truevfs.kernel.spec.FsCompositeDriver;
import net.java.truevfs.kernel.spec.FsController;
import net.java.truevfs.kernel.spec.FsFilteringManager;
import net.java.truevfs.kernel.spec.FsManager;
import net.java.truevfs.kernel.spec.FsManagerTestSuite;
import net.java.truevfs.kernel.spec.FsMountPoint;
import net.java.truevfs.kernel.spec.FsSimpleCompositeDriver;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

/**
 * @author Christian Schlichtherle
 */
public class FsFilteringManagerTest extends FsManagerTestSuite {

    @Override
    protected FsManager newManager(Type type) {
        return new FsFilteringManager(
                new DefaultManager(type),
                FsMountPoint.create(URI.create("file:/")));
    }

    @Test
    public void testFiltering() {
        final FsCompositeDriver driver = new FsSimpleCompositeDriver(
                new MockDriverMapContainer("file|tar|tar.gz|zip"));
        for (final String[][] params : new String[][][] {
            // { { /* filter */ }, { /* test set */ }, { /* result set */ } },
            { { "tar:file:/bar.tar!/" }, { "file:/", "tar:file:/bar.tar!/", "tar.gz:file:/bar.tar.gz!/" }, { "tar:file:/bar.tar!/" } },
            { { "tar:zip:file:/foo.zip!/bar.tar!/" }, { "file:/", "zip:file:/foo.zip!/", "tar:file:/bar.tar!/" }, { } },
            { { "file:/foo.zip/bar.tar/" }, { "file:/", "zip:file:/foo.zip!/", "tar:file:/bar.tar!/" }, { } },
            { { "tar:file:/foo!/" }, { "file:/", "zip:file:/foo!/", "tar:file:/bar!/" }, { "zip:file:/foo!/" } },
            { { "zip:file:/foo.zip!/" }, { "file:/", "zip:file:/foo.zip!/", "tar:file:/bar.tar!/" }, { "zip:file:/foo.zip!/" } },
            { { "file:/foo.zip/" }, { "file:/", "zip:file:/foo.zip!/", "tar:file:/bar.tar!/" }, { } },
            { { "file:/" }, { "file:/", "zip:file:/foo.zip!/", "tar:file:/bar.tar!/" }, { "file:/", "zip:file:/foo.zip!/", "tar:file:/bar.tar!/" } },
        }) {
            assert params[0].length == 1;

            final FsManager manager = new DefaultManager(STRONG);
            for (final String param : params[1])
                manager.controller(driver, FsMountPoint.create(URI.create(param)));
            assertThat(manager.size(), is(params[1].length));

            final Set<FsMountPoint> set = new HashSet<>();
            for (final String param : params[2])
                set.add(FsMountPoint.create(URI.create(param)));

            final FsManager filter = new FsFilteringManager(
                    manager, FsMountPoint.create(URI.create(params[0][0])));
            assertThat(filter.size(), is(params[2].length));
            for (final FsController controller : filter)
                assertTrue(set.contains(controller.getModel().getMountPoint()));
        }
    }
}