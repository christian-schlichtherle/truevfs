/*
 * Copyright 2011 Schlichtherle IT Services
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
package de.schlichtherle.truezip.fs;

import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import java.io.Serializable;
import net.jcip.annotations.Immutable;
import net.jcip.annotations.ThreadSafe;

/**
 * A factory for thread-safe file system controllers.
 * 
 * @author  Christian Schlichtherle
 * @version $Id$
 */
public interface FsDriver {

    /**
     * Returns {@code true} iff this driver implements a federated file
     * system type, i.e. if the type of file system must be a member of a
     * parent file system.
     * E.g. the file system drivers for the ZIP or TAR file formats would
     * return {@code true} because a ZIP or TAR file must be a member of
     * another file system, e.g. a regular directory.
     * To the contrary, the driver for the file scheme would return
     * {@code false} because this file system type cannot be a member of a
     * parent file system.
     * 
     * @return {@code true} iff the type of the file system implemented by this
     *         driver is federated, i.e. must be a member of a parent file
     *         system.
     */
    boolean isFederated();

    /**
     * Returns a new thread-safe file system controller for the given mount
     * point and parent file system controller.
     * <p>
     * When called, the following expression is a precondition:
     * {@code
            null == mountPoint.getParent()
                    ? null == parent
                    : mountPoint.getParent().equals(parent.getModel().getMountPoint())
     * }
     *
     * @param  mountPoint the mount point of the file system.
     * @param  parent the parent file system controller.
     * @return A new thread-safe file system controller for the given mount
     *         point and parent file system controller.
     */
    @NonNull FsController<?>
    newController(  @NonNull  FsMountPoint mountPoint,
                    @Nullable FsController<?> parent);
}
