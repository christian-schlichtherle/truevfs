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

import edu.umd.cs.findbugs.annotations.DefaultAnnotation;
import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import net.jcip.annotations.Immutable;

/**
 * A factory for thread-safe file system controllers.
 * 
 * @author  Christian Schlichtherle
 * @version $Id$
 */
@Immutable
public abstract class FsDriver {

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
    public abstract @NonNull FsController<?>
    newController(  @NonNull  FsMountPoint mountPoint,
                    @Nullable FsController<?> parent);

    /**
     * Returns {@code true} iff this file system driver implements a federated
     * file system type, i.e. if the type of file system must be a member of a
     * parent file system.
     * <p>
     * The implementation in the class {@link FsDriver} returns {@code false}.
     * 
     * @return {@code true} iff the type of the file system implemented by this
     *         file system driver is federated, i.e. must be a member of a
     *         parent file system.
     */
    public boolean isFederated() {
        return false;
    }

    @Override
    public String toString() {
        return new StringBuilder()
                .append(getClass().getName())
                .append("[federated=")
                .append(isFederated())
                .append(']')
                .toString();
    }
}
