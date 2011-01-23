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
import de.schlichtherle.truezip.util.SuffixSet;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.DefaultAnnotation;
import edu.umd.cs.findbugs.annotations.NonNull;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.ServiceConfigurationError;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.jcip.annotations.Immutable;

/**
 * Contains all file system drivers found on the class path.
 * Its map of file system drivers is populated by instantiating all classes
 * which are named in the resource files with the name
 * {@code "META-INF/services/de.schlichtherle.truezip.fs.FsDriverService"}
 * on the class path by calling their no-arg constructor.
 * <p>
 * If no file system drivers are found, a {@link ServiceConfigurationError} is
 * thrown.
 * <p>
 * Note that the kernel classes have no dependency on this class; so using
 * this service locator is completely optional for a pure kernel application.
 *
 * @author  Christian Schlichtherle
 * @version $Id$
 */
@Immutable
@DefaultAnnotation(NonNull.class)
public final class FsDriverServices {

    private static final ServiceLocator serviceLocator
            = new ServiceLocator(FsDriverServices.class.getClassLoader());

    /** You cannot instantiate this class. */
    private FsDriverServices() {
    }

    /**
     * A static factory method for an unmodifiable driver map which is
     * constructed from the given configuration.
     * This method is intended to be used by provider implementations of the
     * {@link FsDriverService} interface for convenient creation of the map to
     * return by
     *
     * @param  config
     * @return The new map to use as the return value of
     *         {@link FsDriverService#getDrivers()}.
     */
    public static Map<FsScheme, FsDriver> newMap(final Object[][] config) {
        final Map<FsScheme, FsDriver> drivers = new HashMap<FsScheme, FsDriver>();
        for (final Object[] param : config) {
            final SuffixSet schemes = new SuffixSet((String) param[0]);
            final FsDriver driver = newDriver(param[1]);
            if (schemes.isEmpty())
                throw new IllegalArgumentException("No schemes for " + driver);
            for (String scheme : schemes)
                drivers.put(FsScheme.create(scheme), driver);
        }
        return Collections.unmodifiableMap(drivers);
    }

    @SuppressWarnings("unchecked")
    private static @CheckForNull FsDriver newDriver(@CheckForNull Object driver) {
        try {
            if (driver instanceof String)
                driver = serviceLocator.getClass((String) driver);
            if (driver instanceof Class<?>)
                driver = ((Class<? extends FsDriver>) driver).newInstance();
            return (FsDriver) driver; // may throw ClassCastException
        } catch (Exception ex) {
            throw new IllegalArgumentException(ex); // NOI18N
        }
    }
}
