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
package de.schlichtherle.truezip.io.file;

import de.schlichtherle.truezip.io.filesystem.FileSystemController;
import de.schlichtherle.truezip.io.filesystem.FileSystemDriver;
import de.schlichtherle.truezip.io.filesystem.MountPoint;
import de.schlichtherle.truezip.io.filesystem.Scheme;
import de.schlichtherle.truezip.io.filesystem.file.FileDriver;
import edu.umd.cs.findbugs.annotations.NonNull;

/**
 * @author Christian Schlichtherle
 * @version $Id$
 */
final class ArchiveFileSystemDriver implements FileSystemDriver {
    private static final Scheme FILE_SCHEME = Scheme.create("file");
    private static final FileSystemDriver FILE_DRIVER = new FileDriver();

    private final ArchiveDetector detector;

    ArchiveFileSystemDriver(final @NonNull ArchiveDetector detector) {
        if (null == detector)
            throw new NullPointerException();
        this.detector = detector;
    }

    final @NonNull ArchiveDetector getDetector() {
        return detector;
    }

    /**
     * {@inheritDoc}
     *
     * @throws NullPointerException if the archive detector does not know an
     *         appropriate archive driver for the scheme of the given mount
     *         point.
     */
    @Override
    public FileSystemController<?>
    newController(MountPoint mountPoint, FileSystemController<?> parent) {
        Scheme scheme = mountPoint.getScheme();
        return FILE_SCHEME.equals(scheme)
                ? FILE_DRIVER.newController(mountPoint, parent)
                : detector.getDriver(scheme).newController(mountPoint, parent);
    }
}
