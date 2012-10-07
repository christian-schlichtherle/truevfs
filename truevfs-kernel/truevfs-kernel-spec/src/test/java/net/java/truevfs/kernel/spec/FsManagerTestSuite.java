/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.kernel.spec;

import java.net.URI;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import static net.java.truecommons.shed.Filter.*;
import net.java.truevfs.kernel.driver.mock.MockDriverMapContainer;
import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Christian Schlichtherle
 */
public abstract class FsManagerTestSuite {

    private final FsMetaDriver driver = new FsSimpleMetaDriver(
            new MockDriverMapContainer("file|tar|tar.gz|zip"));
    private FsManager manager;

    @Before
    public void setUp() { manager = newManager(); }

    protected abstract FsManager newManager();

    @Test
    public void testGetControllerWithNull() {
        try {
            newManager().controller(null, null);
            fail();
        } catch (NullPointerException expected) {
        }
    }

    @Test
    public void testForward() throws InterruptedException {
        for (final String[] params : new String[][] {
            {
                "file:/",
                "zip:file:/öuter.zip!/",
                "zip:zip:file:/öuter.zip!/inner.zip!/",
                "zip:zip:zip:file:/öuter.zip!/inner.zip!/nüts.zip!/",
            },
            {
                "file:/",
                "zip:file:/föo.zip!/",
                "zip:zip:file:/föo.zip!/bär.zip!/",
                "zip:zip:zip:file:/föo.zip!/bär.zip!/bäz.zip!/",
            },
        }) {
            FsController parent = null;
            for (final String param : params) {
                final FsMountPoint mountPoint
                        = FsMountPoint.create(URI.create(param));
                final FsController controller
                        = manager.controller(driver, mountPoint);
                if (null != parent && null != parent.getParent())
                    assertThat(controller.getParent(), sameInstance((Object) parent));
                parent = controller;
            }
            try (final FsControllerStream stream = manager.controllers(ACCEPT_ANY)) {
                assertThat(stream.size(), is(params.length));
            }
            try (final FsControllerStream stream = manager.controllers(ACCEPT_NONE)) {
                assertThat(stream.size(), is(0));
            }
            parent = null;
            waitForAllManagersToGetGarbageCollected();
        }
    }

    @Test
    public void testBackward() throws InterruptedException {
        for (final String[] params : new String[][] {
            {
                "zip:zip:zip:file:/öuter.zip!/inner.zip!/nüts.zip!/",
                "zip:zip:file:/öuter.zip!/inner.zip!/",
                "zip:file:/öuter.zip!/",
                "file:/",
            },
            {
                "zip:zip:zip:file:/föo.zip!/bär.zip!/bäz.zip!/",
                "zip:zip:file:/föo.zip!/bär.zip!/",
                "zip:file:/föo.zip!/",
                "file:/",
            },
        }) {
            FsController top = null;
            FsController member = null;
            for (final String param : params) {
                final FsMountPoint mountPoint
                        = FsMountPoint.create(URI.create(param));
                final FsController controller
                        = manager.controller(driver, mountPoint);
                if (null != member && null != controller.getParent())
                    assertThat(controller, sameInstance((Object) member.getParent()));
                member = controller;
                if (null == top) top = controller;
            }
            try (final FsControllerStream stream = manager.controllers(ACCEPT_ANY)) {
                Iterator<FsController> i = stream.iterator();
                for (final String param : params) {
                    final FsMountPoint mountPoint
                            = FsMountPoint.create(URI.create(param));
                    assertThat(i.next().getModel().getMountPoint(), equalTo(mountPoint));
                }
                assertThat(i.hasNext(), is(false));
                assertThat(stream.size(), is(params.length));
                // The iterator must be set to null because otherwise it will
                // prevent the iterated controllers to be garbage collected
                // because the JVM bytecode still references it even though the
                // local variable falls out of scope after the
                // try-with-resources statement (verified with heap dump).
                i = null;
            }
            member = null;
            top = null;
            waitForAllManagersToGetGarbageCollected();
        }
    }

    private void waitForAllManagersToGetGarbageCollected() {
        int size;
        do {
            System.gc(); // triggering GC in a loop seems to help with concurrency!
            try (final FsControllerStream stream = manager.controllers(ACCEPT_ANY)) {
                size = stream.size();
            }
        } while (0 < size);
    }

    @Test
    public void testFiltering() {
        for (final String[][] params : new String[][][] {
            // { { /* filter mount point */ }, { /* input set */ }, { /* output set */ } },
            { { "tar:file:/bar.tar!/" }, { "file:/", "tar:file:/bar.tar!/", "tar.gz:file:/bar.tar.gz!/" }, { "tar:file:/bar.tar!/" } },
            { { "tar:zip:file:/foo.zip!/bar.tar!/" }, { "file:/", "zip:file:/foo.zip!/", "tar:file:/bar.tar!/" }, { } },
            { { "file:/foo.zip/bar.tar/" }, { "file:/", "zip:file:/foo.zip!/", "tar:file:/bar.tar!/" }, { } },
            { { "tar:file:/foo!/" }, { "file:/", "zip:file:/foo!/", "tar:file:/bar!/" }, { "zip:file:/foo!/" } },
            { { "zip:file:/foo.zip!/" }, { "file:/", "zip:file:/foo.zip!/", "tar:file:/bar.tar!/" }, { "zip:file:/foo.zip!/" } },
            { { "file:/foo.zip/" }, { "file:/", "zip:file:/foo.zip!/", "tar:file:/bar.tar!/" }, { } },
            { { "file:/" }, { "file:/", "zip:file:/foo.zip!/", "tar:file:/bar.tar!/" }, { "file:/", "zip:file:/foo.zip!/", "tar:file:/bar.tar!/" } },
        }) {
            assert params[0].length == 1;

            // Create controllers and add them to a set in order to prevent
            // them from getting garbage collected.
            final FsManager manager = newManager();
            final Set<FsController> input = new HashSet<>();
            for (final String param : params[1]) {
                final FsMountPoint mountPoint = FsMountPoint.create(URI.create(param));
                input.add(manager.controller(driver, mountPoint));
            }

            // Assert that the manager has all input controllers mapped.
            try (final FsControllerStream stream = manager.controllers(ACCEPT_ANY)) {
                assertThat(stream.size(), is(params[1].length));
                for (final FsController controller : stream)
                    assertTrue(input.contains(controller));
            }

            final Set<FsMountPoint> output = new HashSet<>();
            for (final String param : params[2]) {
                final FsMountPoint mountPoint = FsMountPoint.create(URI.create(param));
                output.add(mountPoint);
            }

            final FsMountPoint filter = FsMountPoint.create(URI.create(params[0][0]));
            try (final FsControllerStream stream = manager.controllers(new FsControllerFilter(filter))) {
                assertThat(stream.size(), is(params[2].length));
                for (final FsController controller : stream)
                    assertTrue(output.remove(controller.getModel().getMountPoint()));
            }

            assertTrue(output.isEmpty());
        }
    }
}
