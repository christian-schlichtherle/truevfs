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
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

/**
 * @author Christian Schlichtherle
 * @version $Id$
 */
public class FileSystemManagerTest {

    private FileSystemManager manager;
    private FileSystemDriver<?> driver;

    @Before
    public void setUp() {
        manager = new FileSystemManager();
        driver = new Driver();
    }

    @Test
    public void testGetControllerWithNull() {
        try {
            manager.getController(null, null, null);
            fail();
        } catch (NullPointerException expected) {
        }
    }

    @Test
    public void testGetControllerForward() {
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
                        = manager.getController(mountPoint, driver, null);
                if (null != parent && null != parent.getParent())
                    assertThat(controller.getParent(), sameInstance((Object) parent));
                parent = controller;
            }
        }
    }

    @Test
    public void testGetControllerBackward() {
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
                        = manager.getController(mountPoint, driver, null);
                if (null != member && null != controller.getParent())
                    assertThat(controller, sameInstance((Object) member.getParent()));
                member = controller;
            }
        }
    }

    private static class Driver implements FileSystemDriver<FileSystemModel> {
        @Override
        public FileSystemController<?> newController(
                final MountPoint mountPoint,
                final FileSystemController<?> parent) {
            assert null == mountPoint.getParent()
                    ? null == parent
                    : mountPoint.getParent().equals(parent.getModel().getMountPoint());
            final Scheme scheme = mountPoint.getScheme();
            if (Scheme.FILE.equals(scheme)) {
                // FIXME: Replace FileDriver.INSTANCE with a service locator!
                return new FileDriver().newController(mountPoint);
            } else if (Scheme.create("zip").equals(scheme)) {
                return new ZipDriver().newController(mountPoint, parent);
            } else
                throw new IllegalArgumentException();
        }
    }
}
