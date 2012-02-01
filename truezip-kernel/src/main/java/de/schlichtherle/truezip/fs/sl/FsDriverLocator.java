/*
 * Copyright 2004-2012 Schlichtherle IT Services
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package de.schlichtherle.truezip.fs.sl;

import de.schlichtherle.truezip.fs.FsDriver;
import de.schlichtherle.truezip.fs.FsDriverProvider;
import de.schlichtherle.truezip.fs.FsScheme;
import de.schlichtherle.truezip.fs.spi.FsDriverService;
import static de.schlichtherle.truezip.util.Maps.initialCapacity;
import de.schlichtherle.truezip.util.ServiceLocator;
import java.util.*;
import static java.util.logging.Level.CONFIG;
import static java.util.logging.Level.WARNING;
import java.util.logging.Logger;
import net.jcip.annotations.Immutable;

/**
 * Locates all file system drivers found on the class path.
 * The map of file system drivers is populated by instantiating all classes
 * which are named in the resource files with the name
 * {@code "META-INF/services/de.schlichtherle.truezip.fs.spi.FsDriverService"}
 * on the class path by calling their public no-argument constructor.
 * 
 * @see     FsDriverService
 * @author  Christian Schlichtherle
 * @version $Id$
 */
@Immutable
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
    private static final class Boot {
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
                logger.log(WARNING, "null", FsDriverService.class);
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
                        initialCapacity(sorted.size()));
            for (final Map.Entry<FsScheme, FsDriver> entry : sorted.entrySet()) {
                final FsScheme scheme = entry.getKey();
                final FsDriver driver = entry.getValue();
                logger.log(CONFIG, "mapping",
                        new Object[] { scheme, driver });
                fast.put(scheme, driver);
            }
            DRIVERS = Collections.unmodifiableMap(fast);
        }
    } // Boot
}
