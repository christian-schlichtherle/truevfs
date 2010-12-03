/*
 * Copyright 2010 Schlichtherle IT Services
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
package de.schlichtherle.truezip.io.filesystem.file;

import de.schlichtherle.truezip.io.filesystem.ComponentFileSystemController;
import de.schlichtherle.truezip.io.filesystem.FileSystemController;
import de.schlichtherle.truezip.io.filesystem.FileSystemDriver;
import de.schlichtherle.truezip.io.filesystem.FileSystemModel;
import java.net.URI;

/**
 * @author  Christian Schlichtherle
 * @version $Id$
 */
public final class FileDriver implements FileSystemDriver<FileSystemModel> {

    /** The default instance. FIXME: Remove this! */
    public static final FileDriver INSTANCE
            = new FileDriver();

    private FileDriver() {
    }

    public FileSystemModel newModel(URI mountPoint) {
        return new FileSystemModel(mountPoint, null);
    }

    public ComponentFileSystemController<FileEntry> newController(
            FileSystemModel model) {
        return new FileController(model);
    }

    @Override
    public FileSystemModel newModel(URI mountPoint, FileSystemModel parent) {
        if (null != parent)
            throw new IllegalArgumentException();
        return new FileSystemModel(mountPoint, null);
    }

    @Override
    public FileSystemController<FileEntry> newController(
            FileSystemModel model,
            ComponentFileSystemController<?> parent) {
        if (null != parent)
            throw new IllegalArgumentException();
        return new FileController(model);
    }
}
