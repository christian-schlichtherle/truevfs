/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.truezip.kernel.sl;

import de.truezip.kernel.fs.FsDriver;
import de.truezip.kernel.fs.FsDriverProvider;
import de.truezip.kernel.fs.addr.FsScheme;
import de.truezip.kernel.fs.spi.FsDriverService;
import static de.truezip.kernel.util.Maps.initialCapacity;
import de.truezip.kernel.util.ServiceLocator;
import java.util.*;
import static java.util.logging.Level.CONFIG;
import static java.util.logging.Level.WARNING;
import java.util.logging.Logger;
import javax.annotation.concurrent.Immutable;

/**
 * Locates all file system drivers on the class path.
 * The map of file system drivers is populated by instantiating all classes
 * which are named in the resource files with the name
 * {@code "META-INF/services/de.truezip.kernel.fs.spi.FsDriverService"}
 * on the class path by calling their public no-argument constructor.
 * 
 * @see    FsDriverService
 * @author Christian Schlichtherle
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