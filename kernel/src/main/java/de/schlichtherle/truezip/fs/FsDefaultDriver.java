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
import java.util.Map;

/**
 * Uses a given file system driver provider to lookup the appropriate driver
 * for the scheme of a given mount point.
 *
 * @author  Christian Schlichtherle
 * @version $Id$
 */
public final class FsDefaultDriver implements FsFederatingDriver {

    /**
     * Equivalent to
     * {@code new FsDefaultDriver(FsClassPathDriverProvider.INSTANCE)}.
     */
    public static final FsDefaultDriver
            ALL = new FsDefaultDriver(FsClassPathDriverProvider.INSTANCE);

    private final Map<FsScheme, ? extends FsDriver> drivers;

    /**
     * Constructs a new file system meta driver which qill query the given
     * file system provider for an appropriate file system driver.
     */
    public FsDefaultDriver(final @NonNull FsDriverProvider provider) {
        this.drivers = provider.getDrivers(); // immutable map!
        if (null == drivers)
            throw new NullPointerException("broken interface contract!");
    }

    @Override
    public FsController<?>
    newController(FsMountPoint mountPoint, FsController<?> parent) {
        assert null == mountPoint.getParent()
                ? null == parent
                : mountPoint.getParent().equals(parent.getModel().getMountPoint());
        return drivers.get(mountPoint.getScheme()).newController(mountPoint, parent);
    }
}
