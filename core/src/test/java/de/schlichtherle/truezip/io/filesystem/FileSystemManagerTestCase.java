/*
 * Copyright (C) 2010 Schlichtherle IT Services
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.schlichtherle.truezip.io.filesystem;

import java.util.Iterator;
import de.schlichtherle.truezip.io.filesystem.file.FileDriver;
import de.schlichtherle.truezip.io.archive.driver.zip.ZipDriver;
import java.net.URI;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

/**
 * @author Christian Schlichtherle
 * @version $Id$
 */
public abstract class FileSystemManagerTestCase {

    protected FileSystemManager manager;
    final FileSystemDriver driver = new Driver();

    @Before
    public void setUp() {
    }

    @After
    public final void tearDown() {
        manager = null;
    }

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
            FileSystemController<?> parent = null;
            for (final String param : params) {
                final MountPoint mountPoint
                        = MountPoint.create(URI.create(param));
                final FileSystemController<?> controller
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
            FileSystemController<?> member = null;
            for (final String param : params) {
                final MountPoint mountPoint
                        = MountPoint.create(URI.create(param));
                final FileSystemController<?> controller
                        = manager.getController(mountPoint, driver);
                if (null != member && null != controller.getParent())
                    assertThat(controller, sameInstance((Object) member.getParent()));
                member = controller;
            }

            Iterator<FileSystemController<?>> i = manager.iterator();
            for (final String param : params) {
                final MountPoint mountPoint
                        = MountPoint.create(URI.create(param));
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
            Logger.getLogger(FileSystemManagerTestCase.class.getName()).log(Level.WARNING, "Current thread was interrupted while waiting!", ex);
        }
    }

    /** Represents the "file" URI scheme. */
    private static final Scheme FILE = Scheme.create("file");

    static class Driver implements FileSystemDriver {
        @Override
        public FileSystemController<?> newController(
                final MountPoint mountPoint,
                final FileSystemController<?> parent) {
            assert null == mountPoint.getParent()
                    ? null == parent
                    : mountPoint.getParent().equals(parent.getModel().getMountPoint());
            final Scheme scheme = mountPoint.getScheme();
            if (FILE.equals(scheme)) {
                return new FileDriver().newController(mountPoint);
            } else if (Scheme.create("zip").equals(scheme)) {
                return new ZipDriver().newController(mountPoint, parent);
            } else
                throw new IllegalArgumentException();
        }
    }
}
