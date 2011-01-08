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

import de.schlichtherle.truezip.io.fs.FsController;
import de.schlichtherle.truezip.io.fs.FSDriver1;
import de.schlichtherle.truezip.io.fs.FSMountPoint1;
import de.schlichtherle.truezip.io.fs.FSScheme1;
import de.schlichtherle.truezip.io.fs.file.FSFileDriver;
import edu.umd.cs.findbugs.annotations.NonNull;

/**
 * Adapts the {@link ArchiveDetector} interface to the {@link FSDriver1}
 * interface for use with the {@link File} class.
 *
 * @author Christian Schlichtherle
 * @version $Id$
 */
final class ArchiveDetectorFSDriver implements FSDriver1 {

    private static final FSScheme1 FILE_SCHEME = FSScheme1.create("file");
    private static final FSDriver1 FILE_DRIVER = new FSFileDriver();

    private final ArchiveDetector detector;

    ArchiveDetectorFSDriver() {
        this(ArchiveDetector.ALL);
    }

    ArchiveDetectorFSDriver(final @NonNull ArchiveDetector detector) {
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
    public FsController<?>
    newController(FSMountPoint1 mountPoint, FsController<?> parent) {
        FSScheme1 scheme = mountPoint.getScheme();
        return FILE_SCHEME.equals(scheme)
                ? FILE_DRIVER.newController(mountPoint, parent)
                : detector.getDriver(scheme).newController(mountPoint, parent);
    }
}
