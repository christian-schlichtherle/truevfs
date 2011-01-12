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

import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import java.util.Map;
import net.jcip.annotations.Immutable;

/**
 * A federating file system driver queries the scheme of the given mount point
 * in order to lookup the appropriate file system driver which is then used to
 * create the requested file system controller.
 * <p>
 * Implementations must be thread-safe.
 *
 * @author  Christian Schlichtherle
 * @version $Id$
 */
public interface FsFederatingDriver extends FsDriver {

    /**
     * {@inheritDoc}
     * <p>
     * The file system controller is created by using a file system driver
     * which is looked up by querying the scheme of the given mount point.
     *
     * @throws ServiceConfigurationError if no appropriate file system driver
     *         is found for the scheme of the given mount point.
     */
    @Override
    @NonNull FsController<?>
    newController(  @NonNull FsMountPoint mountPoint,
                    @CheckForNull FsController<?> parent);
}
