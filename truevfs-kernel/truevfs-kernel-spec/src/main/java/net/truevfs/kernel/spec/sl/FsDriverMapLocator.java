/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.truevfs.kernel.spec.sl;

import java.util.*;
import static java.util.logging.Level.CONFIG;
import static java.util.logging.Level.WARNING;
import java.util.logging.Logger;
import javax.annotation.concurrent.Immutable;
import net.truevfs.kernel.spec.FsDriver;
import net.truevfs.kernel.spec.FsDriverMapProvider;
import net.truevfs.kernel.spec.FsScheme;
import net.truevfs.kernel.spec.spi.FsDriverMapFactory;
import static net.truevfs.kernel.spec.util.HashMaps.initialCapacity;
import net.truevfs.kernel.spec.util.ServiceLocator;

/**
 * Locates all file system drivers on the class path.
 * The map of file system drivers is populated by instantiating all classes
 * which are named in the resource files with the name
 * {@code "META-INF/services/net.truevfs.kernel.spi.FsDriverMapFactory"}
 * on the class path by calling their public no-argument constructor.
 * 
 * @see    FsDriverMapFactory
 * @author Christian Schlichtherle
 */
@Immutable
public final class FsDriverMapLocator implements FsDriverMapProvider {

    /** The singleton instance of this class. */
    public static final FsDriverMapLocator SINGLETON = new FsDriverMapLocator();

    /** Can't touch this - hammer time! */
    private FsDriverMapLocator() { }

    @Override
    public Map<FsScheme, FsDriver> drivers() {
        return Boot.DRIVERS;
    }

    /** A static data utility class used for lazy initialization. */
    private static final class Boot {
        static final Map<FsScheme, FsDriver> DRIVERS;
        static {
            final Logger logger = Logger.getLogger(
                    FsDriverMapLocator.class.getName(),
                    FsDriverMapLocator.class.getName());
            final Iterator<FsDriverMapFactory>
                    i = new ServiceLocator(FsDriverMapLocator.class.getClassLoader())
                        .getServices(FsDriverMapFactory.class);
            if (!i.hasNext())
                logger.log(WARNING, "null", FsDriverMapFactory.class);
            final Collection<FsDriverMapFactory> services = new LinkedList<>();
            while (i.hasNext()) {
                FsDriverMapFactory service = i.next();
                logger.log(CONFIG, "located", service);
                services.add(service);
            }
            final FsDriverMapFactory[] prioritized
                    = services.toArray(new FsDriverMapFactory[services.size()]);
            Arrays.sort(prioritized, new Comparator<FsDriverMapFactory>() {
                @Override
                public int compare(FsDriverMapFactory o1, FsDriverMapFactory o2) {
                    return o1.getPriority() - o2.getPriority();
                }
            });
            final Map<FsScheme, FsDriver> sorted = new TreeMap<>();
            for (final FsDriverMapFactory service : prioritized) {
                for (final Map.Entry<FsScheme, FsDriver> entry
                        : service.drivers().entrySet()) {
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
