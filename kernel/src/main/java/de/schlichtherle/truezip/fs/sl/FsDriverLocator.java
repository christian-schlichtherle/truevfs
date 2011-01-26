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
package de.schlichtherle.truezip.fs.sl;

import de.schlichtherle.truezip.fs.FsDriver;
import de.schlichtherle.truezip.fs.FsScheme;
import de.schlichtherle.truezip.fs.FsDriverService;
import de.schlichtherle.truezip.fs.spi.FsDriverProvider;
import de.schlichtherle.truezip.util.ServiceLocator;
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
 * Locates all file system drivers found on the class path.
 * The map of file system drivers is populated by instantiating all classes
 * which are named in the resource files with the name
 * {@code "META-INF/services/de.schlichtherle.truezip.fs.spi.FsDriverProvider"}
 * on the class path by calling their no-arg constructor.
 * <p>
 * If no file system drivers are found, a {@link ServiceConfigurationError} is
 * thrown.
 *
 * @author  Christian Schlichtherle
 * @version $Id$
 */
@Immutable
@DefaultAnnotation(NonNull.class)
public final class FsDriverLocator implements FsDriverService {

    /** The singleton instance of this class. */
    public static final FsDriverLocator SINGLETON = new FsDriverLocator();

    private final Map<FsScheme, FsDriver> drivers;

    /** You cannot instantiate this class. */
    private FsDriverLocator() {
        final Logger
                logger = Logger.getLogger(  FsDriverLocator.class.getName(),
                                            FsDriverLocator.class.getName());
        final Iterator<FsDriverProvider>
                i = new ServiceLocator(FsDriverLocator.class.getClassLoader())
                    .getServices(FsDriverProvider.class);
        final Map<FsScheme, FsDriver>
                drivers = new HashMap<FsScheme, FsDriver>();
        if (!i.hasNext())
            throw new ServiceConfigurationError(
                    "No service providers available for " + FsDriverProvider.class);
        while (i.hasNext()) {
            FsDriverProvider provider = i.next();
            logger.log(Level.CONFIG, "located", provider);
            for (final Map.Entry<FsScheme, FsDriver> entry
                    : provider.getDrivers().entrySet()) {
                final FsScheme scheme = entry.getKey();
                final FsDriver driver = entry.getValue();
                if (null != scheme && null != driver) {
                    drivers.put(scheme, driver);
                    logger.log(Level.CONFIG, "mapped",
                            new Object[] { scheme, driver });
                }
            }
        }
        this.drivers = Collections.unmodifiableMap(drivers);
    }

    @Override
    public Map<FsScheme, FsDriver> getDrivers() {
        return drivers;
    }
}
