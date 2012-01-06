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
import java.util.Iterator;
import edu.umd.cs.findbugs.annotations.NonNull;
import java.net.URI;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

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
        manager = newManager();
    }

    protected abstract @NonNull FsManager newManager();

    @Test
    public void testGetControllerWithNull() {
        try {
            manager.getController(null, null);
            fail();
        } catch (NullPointerException expected) {
        }
    }

    @Test
    public void testForward() {
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
    public void testBackward() {
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
            FsController<?> member = null;
            for (final String param : params) {
                final FsMountPoint mountPoint
                        = FsMountPoint.create(URI.create(param));
                final FsController<?> controller
                        = manager.getController(mountPoint, driver);
                if (null != member && null != controller.getParent())
                    assertThat(controller, sameInstance((Object) member.getParent()));
                member = controller;
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
            gc();
            assertThat(manager.getSize(), is(0));
        }
    }

    static void gc() {
        System.gc();
        try {
            Thread.sleep(50);
        } catch (InterruptedException ex) {
            Logger.getLogger(FsManagerTestSuite.class.getName()).log(Level.WARNING, "Current thread was interrupted while waiting!", ex);
        }
    }
}
