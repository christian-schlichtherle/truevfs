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
package de.schlichtherle.truezip.io.fs;

import java.util.Map;
import net.jcip.annotations.Immutable;

/**
 * @author  Christian Schlichtherle
 * @version $Id$
 */
final class FsFederatingDriver implements FsDriver {

    private final Map<FsScheme, ? extends FsDriver> drivers;

    FsFederatingDriver(final FsDriverProvider provider) {
        this.drivers = provider.getDrivers();
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
