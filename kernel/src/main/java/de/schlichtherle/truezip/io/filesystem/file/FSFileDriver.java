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

import de.schlichtherle.truezip.io.filesystem.FSController;
import de.schlichtherle.truezip.io.filesystem.FSDriver;
import de.schlichtherle.truezip.io.filesystem.FSModel;
import de.schlichtherle.truezip.io.filesystem.FSMountPoint;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.NonNull;

/**
 * @author  Christian Schlichtherle
 * @version $Id$
 */
public final class FSFileDriver implements FSDriver {

    public FSController<?> newController(
            FSMountPoint mountPoint) {
        return new FSFileController(new FSModel(mountPoint));
    }

    @Override
    @NonNull
    public FSController<?> newController(
            @NonNull FSMountPoint mountPoint,
            @CheckForNull FSController<?> parent) {
        if (null != parent)
            throw new IllegalArgumentException();
        return new FSFileController(new FSModel(mountPoint));
    }
}
