/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.truezip.kernel.sl;

import de.truezip.kernel.FsDriver;
import de.truezip.kernel.FsDriverProvider;
import de.truezip.kernel.FsScheme;
import de.truezip.kernel.spi.FsDriverService;
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
 * {@code "META-INF/services/de.truezip.kernel.spi.FsDriverService"}
 * on the class path by calling their public no-argument constructor.
 * 
 * @see    FsDriverService
 * @author Christian Schlichtherle
 */
@Immutable
public final class FsDriverLocator implements FsDriverProvider {

    /** The singleton instance of this class. */
    public static final FsDriverLocator SINGLETON = new FsDriverLocator();

    /** Can't touch this - hammer time! */
    private FsDriverLocator() { }

    @Override
    public Map<FsScheme, FsDriver> getDrivers() {
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
            if (!i.hasNext())
                logger.log(WARNING, "null", FsDriverService.class);
            final Collection<FsDriverService> services = new LinkedList<>();
            while (i.hasNext()) {
                FsDriverService service = i.next();
                logger.log(CONFIG, "located", service);
                services.add(service);
            }
            final FsDriverService[] prioritized
                    = services.toArray(new FsDriverService[services.size()]);
            Arrays.sort(prioritized, new Comparator<FsDriverService>() {
                @Override
                public int compare(FsDriverService o1, FsDriverService o2) {
                    return o1.getPriority() - o2.getPriority();
                }
            });
            final Map<FsScheme, FsDriver> sorted = new TreeMap<>();
            for (final FsDriverService service : prioritized) {
                for (final Map.Entry<FsScheme, FsDriver> entry
                        : service.getDrivers().entrySet()) {
                    final FsScheme scheme = entry.getKey();
                    final FsDriver driver = entry.getValue();
                    if (null != scheme)
                        if (null != driver)
                            sorted.put(scheme, driver);
                        else
                            sorted.remove(scheme);
                }
            }
            final Map<FsScheme, FsDriver> fast
                    = new LinkedHashMap<>(initialCapacity(sorted.size()));
            for (final Map.Entry<FsScheme, FsDriver> entry : sorted.entrySet()) {
                final FsScheme scheme = entry.getKey();
                final FsDriver driver = entry.getValue();
                logger.log(CONFIG, "mapping", new Object[] { scheme, driver });
                fast.put(scheme, driver);
            }
            DRIVERS = Collections.unmodifiableMap(fast);
        }
    } // Boot
}
