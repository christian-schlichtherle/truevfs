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

import de.schlichtherle.truezip.io.filesystem.file.FileDriver;
import de.schlichtherle.truezip.io.archive.driver.zip.ZipDriver;
import java.net.URI;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author Christian Schlichtherle
 * @version $Id$
 */
public class FileSystemManagerTest {

    private FederatedFileSystemManager manager;
    private FileSystemDriver<FileSystemModel> driver;

    @Before
    public void setUp() {
        manager = new FederatedFileSystemManager();
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
    public void testGetControllerWithOpaqueMountPoint() {
        for (final String[] params : new String[][] {
            { "file:/" },
            { "zip:file:/outer.zip!/" },
            { "zip:zip:file:/outer.zip!/inner.zip!/" },
            { "zip:zip:zip:file:/outer.zip!/inner.zip!/nuts.zip!/" },
        }) {
            final FederatedFileSystemController<?> controller
                    = manager.getController(MountPoint.create(URI.create(params[0])), driver);
        }
    }

    @Test
    public void testGetControllerWithHierarchicalMountPoint() {
        final FileSystemDriver<?> file = new FileDriver();
        final FileSystemDriver<?> zip = new ZipDriver();
        for (final Object[] params : new Object[][] {
            { zip, "zip:zip:zip:file:/outer.zip!/inner.zip!/nuts.zip!/", zip, "zip:zip:file:/outer.zip!/inner.zip!/", zip, "zip:file:/outer.zip!/", file, "file:/" },
        }) {
            FederatedFileSystemController<?> controller = null;
            for (int i = params.length; 0 <= --i; ) {
                final MountPoint mountPoint = MountPoint.create(URI.create((String) params[i--]));
                final FileSystemDriver<?> driver = (FileSystemDriver<?>) params[i];
                controller = manager.getController(mountPoint, driver, controller);
            }
        }
    }

    private static class Driver implements FileSystemDriver<FileSystemModel> {

        @Override
        public FileSystemController<?> newController(
                final MountPoint mountPoint,
                final FederatedFileSystemController<?> parent) {
            assert null == mountPoint.getParent()
                    ? null == parent
                    : mountPoint.getParent() == parent.getModel().getMountPoint();
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
