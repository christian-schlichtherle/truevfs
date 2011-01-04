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
package de.schlichtherle.truezip.io.filesystem;

import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;

/**
 * A factory for file system controllers.
 * 
 * @author  Christian Schlichtherle
 * @version $Id$
 */
public interface FSDriver {

    /**
     * Returns a new file system controller for the given mount point
     * and parent file system controller.
     * <p>
     * When called, the following expression is a precondition:
     * {@code
            null == mountPoint.getParent()
                    ? null == parent
                    : mountPoint.getParent().equals(parent.getModel().getMountPoint())
     * }
     * <p>
     * Note that the returned file system controller must be thread-safe!
     *
     * @param  mountPoint the mount point of the file system.
     * @param  parent the parent file system controller.
     * @return A new file system controller for the given mount point and
     *         parent file system controller.
     */
    @NonNull FSController<?>
    newController(  @NonNull FSMountPoint mountPoint,
                    @Nullable FSController<?> parent);
}
