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
import de.schlichtherle.truezip.util.BitField;
import de.schlichtherle.truezip.util.Link.Type;
import static de.schlichtherle.truezip.util.Link.Type.STRONG;
import static de.schlichtherle.truezip.util.Link.Type.WEAK;
import edu.umd.cs.findbugs.annotations.NonNull;
import java.net.URI;
import java.util.Iterator;
import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import org.junit.Before;
import org.junit.Test;

/**
 * @author  Christian Schlichtherle
 * @version $Id$
 */
public abstract class FsManagerTestSuite {

    private final FsCompositeDriver driver
            = new FsDefaultDriver(new DummyDriverService("file|zip"));
    private FsManager manager;

    @Before
    public void setUp() {
        manager = newManager(WEAK);
    }

    protected abstract @NonNull FsManager newManager(Type type);

    @Test
    public void testGetControllerWithNull() {
        for (final Type type : BitField.allOf(Type.class)) {
            try {
                newManager(type).getController(null, null);
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
                        = manager.getController(mountPoint, driver);
                if (null != parent && null != parent.getParent())
                    assertThat(controller.getParent(), sameInstance((Object) parent));
                parent = controller;
            }

            assertThat(manager.getSize(), is(params.length));
            parent = null;
            gc();
            assertThat(manager.getSize(), is(0));
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
                        = manager.getController(mountPoint, driver);
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

            assertThat(manager.getSize(), is(params.length));
            member = null;
            i = null;
            top = null;
            gc();
            assertThat(manager.getSize(), is(0));
        }
    }

    private static void gc() throws InterruptedException {
        System.gc();
        Thread.sleep(50);
    }
}
