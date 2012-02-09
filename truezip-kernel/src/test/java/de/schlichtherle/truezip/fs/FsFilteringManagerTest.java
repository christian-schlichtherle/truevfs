/*
 * Copyright 2004-2012 Schlichtherle IT Services
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package de.schlichtherle.truezip.fs;

import de.schlichtherle.truezip.fs.spi.DummyDriverService;
import de.schlichtherle.truezip.util.Link.Type;
import static de.schlichtherle.truezip.util.Link.Type.STRONG;
import java.net.URI;
import java.util.HashSet;
import java.util.Set;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

/**
 * @author  Christian Schlichtherle
 * @version $Id$
 */
public class FsFilteringManagerTest extends FsManagerTestSuite {

    @Override
    protected FsManager newManager(Type type) {
        return new FsFilteringManager(
                new FsDefaultManager(type),
                FsMountPoint.create(URI.create("file:/")));
    }

    @Test
    public void testFiltering() {
        final FsCompositeDriver driver = new FsDefaultDriver(
                new DummyDriverService("file|tar|tar.gz|zip"));
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

            final FsManager manager = new FsDefaultManager(STRONG);
            for (String param : params[1])
                manager.getController(  FsMountPoint.create(URI.create(param)),
                                        driver);
            assertThat(manager.getSize(), is(params[1].length));

            final Set<FsMountPoint> set = new HashSet<FsMountPoint>();
            for (String param : params[2])
                set.add(FsMountPoint.create(URI.create(param)));

            final FsManager filter = new FsFilteringManager(
                    manager, FsMountPoint.create(URI.create(params[0][0])));
            assertThat(filter.getSize(), is(params[2].length));
            for (FsController<?> controller : filter)
                assertTrue(set.contains(controller.getModel().getMountPoint()));
        }
    }
}
