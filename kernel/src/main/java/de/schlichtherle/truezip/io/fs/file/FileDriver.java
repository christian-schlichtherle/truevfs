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
import de.schlichtherle.truezip.io.fs.FsDriver;
import de.schlichtherle.truezip.io.fs.FsModel;
import de.schlichtherle.truezip.io.fs.FsMountPoint;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.NonNull;

/**
 * @author  Christian Schlichtherle
 * @version $Id$
 */
public final class FileDriver implements FsDriver {

    public FsController<?> newController(
            FsMountPoint mountPoint) {
        return new FileController(new FsModel(mountPoint));
    }

    @Override
    @NonNull
    public FsController<?> newController(
            @NonNull FsMountPoint mountPoint,
            @CheckForNull FsController<?> parent) {
        if (null != parent)
            throw new IllegalArgumentException();
        return new FileController(new FsModel(mountPoint));
    }
}
