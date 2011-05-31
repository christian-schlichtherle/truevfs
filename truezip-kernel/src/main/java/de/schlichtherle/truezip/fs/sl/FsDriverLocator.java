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
import de.schlichtherle.truezip.fs.FsDriverProvider;
import de.schlichtherle.truezip.fs.spi.FsDriverService;
import de.schlichtherle.truezip.util.ServiceLocator;
import edu.umd.cs.findbugs.annotations.DefaultAnnotation;
import edu.umd.cs.findbugs.annotations.NonNull;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.ServiceConfigurationError;
import java.util.TreeMap;
import static java.util.logging.Level.*;
import java.util.logging.Logger;
import net.jcip.annotations.Immutable;

/**
 * Locates all file system drivers found on the class path.
 * The map of file system drivers is populated by instantiating all classes
 * which are named in the resource files with the name
 * {@code "META-INF/services/de.schlichtherle.truezip.fs.spi.FsDriverService"}
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
public final class FsDriverLocator implements FsDriverProvider {

    /** The singleton instance of this class. */
    public static final FsDriverLocator SINGLETON = new FsDriverLocator();

    /** You cannot instantiate this class. */
    private FsDriverLocator() {
    }

    @Override
    public Map<FsScheme, FsDriver> get() {
        return Boot.DRIVERS;
    }

    /** A static data utility class used for lazy initialization. */
    private static class Boot {
        static final Map<FsScheme, FsDriver> DRIVERS;
        static {
            final Logger logger = Logger.getLogger(
                    FsDriverLocator.class.getName(),
                    FsDriverLocator.class.getName());
            final Iterator<FsDriverService>
                    i = new ServiceLocator(FsDriverLocator.class.getClassLoader())
                        .getServices(FsDriverService.class);
            final Map<FsScheme, FsDriver>
                    sorted = new TreeMap<FsScheme, FsDriver>();
            if (!i.hasNext())
                logger.log(WARNING, "none", FsDriverService.class);
            while (i.hasNext()) {
                FsDriverService service = i.next();
                logger.log(CONFIG, "located", service);
                for (final Map.Entry<FsScheme, FsDriver> entry
                        : service.get().entrySet()) {
                    final FsScheme scheme = entry.getKey();
                    final FsDriver newDriver = entry.getValue();
                    if (null != scheme && null != newDriver) {
                        final FsDriver oldDriver = sorted.put(scheme, newDriver);
                        if (null != oldDriver
                                && oldDriver.getPriority() > newDriver.getPriority())
                            sorted.put(scheme, oldDriver);
                    }
                }
            }
            final Map<FsScheme, FsDriver>
                    fast = new LinkedHashMap<FsScheme, FsDriver>(
                        sorted.size() * 4 / 3 + 1);
            for (final Map.Entry<FsScheme, FsDriver> entry : sorted.entrySet()) {
                final FsScheme scheme = entry.getKey();
                final FsDriver driver = entry.getValue();
                logger.log(CONFIG, "mapping",
                        new Object[] { scheme, driver });
                fast.put(scheme, driver);
            }
            DRIVERS = Collections.unmodifiableMap(fast);
        }

        /** Make NetBeans happy. */
        Boot() {
        }
    } // class Boot
}
