/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.kernel.spec;

import java.io.IOException;
import java.net.URI;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import javax.annotation.concurrent.ThreadSafe;
import net.java.truecommons.shed.Filter;
import static net.java.truecommons.shed.Filter.*;
import net.java.truecommons.shed.Visitor;
import net.java.truevfs.kernel.driver.mock.MockDriverMapContainer;
import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Christian Schlichtherle
 */
public abstract class FsManagerTestSuite {

    private final FsCompositeDriver driver = new FsSimpleCompositeDriver(
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
            assertThat(count(ACCEPT_ANY), is(params.length));
            assertThat(count(ACCEPT_NONE), is(0));
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

            final Iterator<String> it = Arrays.asList(params).iterator();
            class ControllerVisitor extends VisitCounter {
                @Override
                public void visit(FsController controller) {
                    final FsMountPoint mountPoint
                            = FsMountPoint.create(URI.create(it.next()));
                    assertThat(controller.getModel().getMountPoint(), equalTo(mountPoint));
                    super.visit(controller);
                }
            }
            assertThat(count(ACCEPT_ANY, new ControllerVisitor()), is(params.length));
            assertThat(it.hasNext(), is(false));

            member = null;
            top = null;
            waitForAllManagersToGetGarbageCollected();
        }
    }

    private void waitForAllManagersToGetGarbageCollected() {
        do {
            System.gc(); // triggering GC in a loop seems to help with concurrency!
        } while (0 < count(ACCEPT_ANY));
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
            manager = newManager();
            final Set<FsController> input = new HashSet<>();
            for (final String param : params[1]) {
                final FsMountPoint mountPoint = FsMountPoint.create(URI.create(param));
                input.add(manager.controller(driver, mountPoint));
            }

            // Assert that the manager has all input controllers mapped.
            class InputVisitor extends VisitCounter {
                @Override
                public void visit(FsController controller) {
                    assertTrue(input.contains(controller));
                    super.visit(controller);
                }
            } // InputVisitor

            assertThat(count(ACCEPT_ANY, new InputVisitor()), is(params[1].length));

            final Set<FsMountPoint> output = new HashSet<>();
            for (final String param : params[2]) {
                final FsMountPoint mountPoint = FsMountPoint.create(URI.create(param));
                output.add(mountPoint);
            }

            class FilterVisitor extends VisitCounter {
                @Override
                public void visit(FsController controller) {
                    assertTrue(output.remove(controller.getModel().getMountPoint()));
                    super.visit(controller);
                }
            }
            final FsMountPoint mountPoint = FsMountPoint.create(URI.create(params[0][0]));
            assertThat(count(new FsControllerFilter(mountPoint), new FilterVisitor()), is(params[2].length));

            assertTrue(output.isEmpty());
        }
    }

    private int count(final Filter<? super FsController> filter) {
        return count(filter, new VisitCounter());
    }

    private int count(
            final Filter<? super FsController> filter,
            final VisitCounter counter) {
        try {
            manager.visit(filter, counter);
        } catch (final IOException ex) {
            throw new AssertionError(ex);
        }
        return counter.get();
    }

    @ThreadSafe
    private static class VisitCounter
    extends AtomicInteger implements Visitor<FsController, IOException> {
        @Override
        public void visit(final FsController controller) {
            assertThat(controller, not(is((FsController) null)));
            incrementAndGet();
        }
    } // VisitCounter
}
