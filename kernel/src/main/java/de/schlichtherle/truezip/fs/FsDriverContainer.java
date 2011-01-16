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
 * Contains all file system drivers found on the class path.
 * Its map of file system drivers is populated by instantiating all classes
 * which are named in the resource files with the name
 * {@code "META-INF/services/de.schlichtherle.truezip.fs.FsDriverService"}
 * on the class path by calling their no-arg constructor.
 * <p>
 * Note that the kernel classes have no dependency on this class; so using
 * this service locator is completely optional for a pure kernel application.
 *
 * @author  Christian Schlichtherle
 * @version $Id$
 */
@Immutable
public final class FsDriverContainer implements FsDriverService {

    /** The singleton instance of this class. */
    public static final FsDriverContainer SINGLETON = new FsDriverContainer();

    private final Map<FsScheme, FsDriver> drivers;

    /** You cannot instantiate this class. */
    private FsDriverContainer() {
        final Iterator<FsDriverService> i
                = new ServiceLocator(FsDriverContainer.class.getClassLoader())
                    .getServices(FsDriverService.class);
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
