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
import java.util.ServiceConfigurationError;

/**
 * Queries the scheme of the mount point of the given file system model in
 * order to lookup the appropriate file system driver which is then used to
 * create the requested thread-safe file system controller.
 *
 * @author  Christian Schlichtherle
 * @version $Id$
 */
public interface FsCompositeDriver {

    /**
     * Returns a new thread-safe file system controller for the mount point of
     * the given file system model and parent file system controller.
     * The file system controller is created by using a file system driver
     * which is looked up by querying the scheme of the mount point of the
     * given file system model.
     * <p>
     * When called, the following expression is a precondition:
     * {@code
            null == model.getParent()
                    ? null == parent
                    : model.getParent().equals(parent.getModel())
     * }
     *
     * @param  model the file system model.
     * @param  parent the nullable parent file system controller.
     * @return A new thread-safe file system controller for the given mount
     *         point and parent file system controller.
     * @throws ServiceConfigurationError if no appropriate file system driver
     *         is found for the scheme of the given mount point.
     * @see    FsDriver#newController
     */
    @NonNull FsController<?>
    newController(@NonNull FsModel model, @Nullable FsController<?> parent);
}
