/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.kernel.fs;

import de.truezip.kernel.fs.*;
import de.truezip.kernel.fs.addr.FsMountPoint;
import de.truezip.kernel.fs.mock.MockDriverService;
import de.truezip.kernel.util.Link.Type;
import static de.truezip.kernel.util.Link.Type.STRONG;
import java.net.URI;
import java.util.HashSet;
import java.util.Set;
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
                new FsArchiveManager(type),
                FsMountPoint.create(URI.create("file:/")));
    }

    @Test
    public void testFiltering() {
        final FsCompositeDriver driver = new FsSimpleCompositeDriver(
                new MockDriverService("file|tar|tar.gz|zip"));
        for (final String[][] params : new String[][][] {
            // { { /* filter */ }, { /* test set */ }, { /* result set */ } },
            { { "file:/" }, { "zip:file:/foo.zip!/", "tar:file:/bar.tar!/" }, { "zip:file:/foo.zip!/", "tar:file:/bar.tar!/" } },
            { { "tar:file:/bar.tar!/" }, { "tar:file:/bar.tar!/", "tar.gz:file:/bar.tar.gz!/" }, { "tar:file:/bar.tar!/" } },
            { { "tar:zip:file:/foo.zip!/bar.tar!/" }, { "zip:file:/foo.zip!/", "tar:file:/bar.tar!/" }, { } },
            { { "file:/foo.zip/bar.tar/" }, { "zip:file:/foo.zip!/", "tar:file:/bar.tar!/" }, { } },
            { { "tar:file:/foo!/" }, { "zip:file:/foo!/", "tar:file:/bar!/" }, { "zip:file:/foo!/" } },
            { { "zip:file:/foo.zip!/" }, { "zip:file:/foo.zip!/", "tar:file:/bar.tar!/" }, { "zip:file:/foo.zip!/" } },
            { { "file:/foo.zip/" }, { "zip:file:/foo.zip!/", "tar:file:/bar.tar!/" }, { } },
            { { "file:/" }, { "zip:file:/foo.zip!/", "tar:file:/bar.tar!/" }, { "zip:file:/foo.zip!/", "tar:file:/bar.tar!/" } },
        }) {
            assert params[0].length == 1;

            final FsManager manager = new FsArchiveManager(STRONG);
            for (final String param : params[1])
                manager.getController(  FsMountPoint.create(URI.create(param)),
                                        driver);
            assertThat(manager.size(), is(params[1].length));

            final Set<FsMountPoint> set = new HashSet<FsMountPoint>();
            for (final String param : params[2])
                set.add(FsMountPoint.create(URI.create(param)));

            final FsManager filter = new FsFilteringManager(
                    manager, FsMountPoint.create(URI.create(params[0][0])));
            assertThat(filter.size(), is(params[2].length));
            for (final FsController<?> controller : filter)
                assertTrue(set.contains(controller.getModel().getMountPoint()));
        }
    }
}