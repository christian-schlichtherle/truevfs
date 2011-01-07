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

import de.schlichtherle.truezip.io.archive.driver.DummyArchiveDriver;
import de.schlichtherle.truezip.io.filesystem.file.FSFileDriver;

/**
 * @author Christian Schlichtherle
 * @version $Id$
 */
final class FSDummyDriver implements FSDriver {

    private static final FSScheme FILE = FSScheme.create("file");

    @Override
    public FSController<?> newController(   final FSMountPoint mountPoint,
                                            final FSController<?> parent) {
        assert null == mountPoint.getParent()
                ? null == parent
                : mountPoint.getParent().equals(parent.getModel().getMountPoint());
        final FSScheme scheme = mountPoint.getScheme();
        if (FILE.equals(scheme)) {
            return new FSFileDriver().newController(mountPoint);
        } else if (FSScheme.create("zip").equals(scheme)) {
            return new DummyArchiveDriver().newController(mountPoint, parent);
        } else {
            throw new IllegalArgumentException();
        }
    }
}
