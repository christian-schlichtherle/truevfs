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

import de.schlichtherle.truezip.util.ServiceLocator;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import net.jcip.annotations.Immutable;

/**
 * @author  Christian Schlichtherle
 * @version $Id$
 */
@Immutable
final class FsClassPathDriverProvider implements FsDriverProvider {

    static final FsClassPathDriverProvider INSTANCE
            = new FsClassPathDriverProvider();

    private final Map<FsScheme, FsDriver> drivers;

    private FsClassPathDriverProvider() {
        final Iterator<FsDriverProvider> i
                = new ServiceLocator(FsClassPathDriverProvider.class.getClassLoader())
                    .getServices(FsDriverProvider.class);
        final Map<FsScheme, FsDriver> drivers = new HashMap<FsScheme, FsDriver>();
        while (i.hasNext())
            drivers.putAll(i.next().getDrivers());
        this.drivers = Collections.unmodifiableMap(drivers);
    }

    @Override
    public Map<FsScheme, FsDriver> getDrivers() {
        return drivers;
    }
}
