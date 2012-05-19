/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.truevfs.kernel;

import net.truevfs.kernel.mock.MockDriverService;
import net.truevfs.kernel.util.BitField;
import net.truevfs.kernel.util.Link.Type;
import static net.truevfs.kernel.util.Link.Type.WEAK;
import java.net.URI;
import java.util.Iterator;
import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import org.junit.Before;
import org.junit.Test;

/**
 * @author  Christian Schlichtherle
 */
public abstract class FsManagerTestSuite {

    private final FsCompositeDriver driver = new FsSimpleCompositeDriver(
            new MockDriverService("file|zip"));
    private FsManager manager;

    @Before
    public void setUp() {
        manager = newManager(WEAK);
    }

    protected abstract FsManager newManager(Type type);

    @Test
    public void testGetControllerWithNull() {
        for (final Type type : BitField.allOf(Type.class)) {
            try {
                newManager(type).controller(null, null);
                fail();
            } catch (NullPointerException expected) {
            }
        }
    }

    @Test
    public void testForward() throws InterruptedException {
        for (final String[] params : new String[][] {
            {
                //"file:/", // does NOT get mapped!
                "zip:file:/öuter.zip!/",
                "zip:zip:file:/öuter.zip!/inner.zip!/",
                "zip:zip:zip:file:/öuter.zip!/inner.zip!/nüts.zip!/",
            },
            {
                //"file:/", // does NOT get mapped!
                "zip:file:/föo.zip!/",
                "zip:zip:file:/föo.zip!/bär.zip!/",
                "zip:zip:zip:file:/föo.zip!/bär.zip!/bäz.zip!/",
            },
        }) {
            FsController<?> parent = null;
            for (final String param : params) {
                final FsMountPoint mountPoint
                        = FsMountPoint.create(URI.create(param));
                final FsController<?> controller
                        = manager.controller(driver, mountPoint);
                if (null != parent && null != parent.getParent())
                    assertThat(controller.getParent(), sameInstance((Object) parent));
                parent = controller;
            }

            assertThat(manager.size(), is(params.length));
            parent = null;
            waitAllManagers();
        }
    }

    @Test
    public void testBackward() throws InterruptedException {
        for (final String[] params : new String[][] {
            {
                "zip:zip:zip:file:/öuter.zip!/inner.zip!/nüts.zip!/",
                "zip:zip:file:/öuter.zip!/inner.zip!/",
                "zip:file:/öuter.zip!/",
                //"file:/", // does NOT get mapped!
            },
            {
                "zip:zip:zip:file:/föo.zip!/bär.zip!/bäz.zip!/",
                "zip:zip:file:/föo.zip!/bär.zip!/",
                "zip:file:/föo.zip!/",
                //"file:/", // does NOT get mapped!
            },
        }) {
            FsController<?> top = null;
            FsController<?> member = null;
            for (final String param : params) {
                final FsMountPoint mountPoint
                        = FsMountPoint.create(URI.create(param));
                final FsController<?> controller
                        = manager.controller(driver, mountPoint);
                if (null != member && null != controller.getParent())
                    assertThat(controller, sameInstance((Object) member.getParent()));
                member = controller;
                if (null == top)
                    top = controller;
            }

            Iterator<FsController<?>> i = manager.iterator();
            for (final String param : params) {
                final FsMountPoint mountPoint
                        = FsMountPoint.create(URI.create(param));
                assertThat(i.next().getModel().getMountPoint(), equalTo(mountPoint));
            }
            assertThat(i.hasNext(), is(false));

            assertThat(manager.size(), is(params.length));
            member = null;
            i = null;
            top = null;
            waitAllManagers();
        }
    }

    private void waitAllManagers() {
        do {
            System.gc(); // triggering GC in a loop seems to help with concurrency!
        } while (0 < manager.size());
    }
}
