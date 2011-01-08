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
package de.schlichtherle.truezip.io.fs.file;

import de.schlichtherle.truezip.io.fs.FsController;
import de.schlichtherle.truezip.io.fs.FSDriver1;
import de.schlichtherle.truezip.io.fs.FSModel1;
import de.schlichtherle.truezip.io.fs.FSMountPoint1;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.NonNull;

/**
 * @author  Christian Schlichtherle
 * @version $Id$
 */
public final class FSFileDriver implements FSDriver1 {

    public FsController<?> newController(
            FSMountPoint1 mountPoint) {
        return new FSFileController(new FSModel1(mountPoint));
    }

    @Override
    @NonNull
    public FsController<?> newController(
            @NonNull FSMountPoint1 mountPoint,
            @CheckForNull FsController<?> parent) {
        if (null != parent)
            throw new IllegalArgumentException();
        return new FSFileController(new FSModel1(mountPoint));
    }
}
