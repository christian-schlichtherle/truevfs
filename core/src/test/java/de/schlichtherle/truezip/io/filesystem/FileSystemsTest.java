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

import de.schlichtherle.truezip.io.archive.driver.zip.ZipDriver;
import de.schlichtherle.truezip.io.archive.model.ArchiveModel;
import de.schlichtherle.truezip.io.filesystem.file.FileController;
import java.net.URI;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author Christian Schlichtherle
 * @version $Id$
 */
public class FileSystemsTest {

    @Test
    public void testGetControllerNull() {
        try {
            FileSystems.getController(null, null, null);
            fail();
        } catch (NullPointerException expected) {
        }
    }

    @Test
    public void testGetControllerOpaque() {
        ComponentFileSystemController<?> controller = FileSystems.getController(
                URI.create("zip:zip:file:/foo!/bar!/baz"),
                null,
                new TestFactory());
    }

    private static class TestFactory
    implements FileSystemDriver<FileSystemModel> {

        @Override
        public FileSystemModel newModel(final URI mountPoint, final FileSystemModel parent) {
            final String scheme = mountPoint.getScheme();
            if ("file".equals(scheme)) {
                return new FileSystemModel(mountPoint, parent, this);
            } else if ("zip".equals(scheme)) {
                return new ArchiveModel(mountPoint, parent, this);
            } else
                throw new IllegalArgumentException();
        }

        @Override
        public FileSystemController<?> newController(final FileSystemModel model, ComponentFileSystemController<?> parentController) {
            if (null == parentController) {
                FileSystemModel parentModel = model.getParent();
                if (null != parentModel)
                    parentController = FileSystems.getController(parentModel.getMountPoint(), null, this);
            }
            final String scheme = model.getMountPoint().getScheme();
            if ("file".equals(scheme)) {
                return new FileController(model);
            } else if ("zip".equals(scheme)) {
                return new ZipDriver().newController((ArchiveModel) model, parentController);
            } else
                throw new IllegalArgumentException();
        }
    }
}
