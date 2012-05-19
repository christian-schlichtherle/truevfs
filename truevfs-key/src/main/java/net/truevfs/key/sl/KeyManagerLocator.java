/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.truevfs.key.sl;

import static net.truevfs.kernel.util.HashMaps.initialCapacity;
import net.truevfs.kernel.util.ServiceLocator;
import net.truevfs.key.AbstractKeyManagerProvider;
import net.truevfs.key.KeyManager;
import net.truevfs.key.spi.KeyManagerService;
import java.util.*;
import static java.util.logging.Level.CONFIG;
import static java.util.logging.Level.WARNING;
import java.util.logging.Logger;
import javax.annotation.concurrent.Immutable;

/**
 * Locates all key managers on the class path.
 * The map of key managers is populated by instantiating all classes
 * which are named in the resource files with the name
 * {@code "META-INF/services/net.truevfs.key.spi.KeyManagerService"}
 * on the class path by calling their public no-argument constructor.
 *
 * @see    KeyManagerService
 * @author Christian Schlichtherle
 */
@Immutable
public final class KeyManagerLocator extends AbstractKeyManagerProvider {

    /** The singleton instance of this class. */
    public static final KeyManagerLocator SINGLETON = new KeyManagerLocator();

    /** Can't touch this - hammer time! */
    private KeyManagerLocator() { }

    @Override
    public Map<Class<?>, KeyManager<?>> getKeyManagers() {
        return Boot.MANAGERS;
    }

    /** A static data utility class used for lazy initialization. */
    private static final class Boot {
        static final Map<Class<?>, KeyManager<?>> MANAGERS;
        static {
            final Logger logger = Logger.getLogger(
                    KeyManagerLocator.class.getName(),
                    KeyManagerLocator.class.getName());
            final Iterator<KeyManagerService>
                    i = new ServiceLocator(KeyManagerLocator.class.getClassLoader())
                        .getServices(KeyManagerService.class);
            if (!i.hasNext())
                logger.log(WARNING, "null", KeyManagerService.class);
            final Collection<KeyManagerService> services = new LinkedList<>();
            while (i.hasNext()) {
                KeyManagerService service = i.next();
                logger.log(CONFIG, "located", service);
                services.add(service);
            }
            final KeyManagerService[] prioritized
                    = services.toArray(new KeyManagerService[services.size()]);
            Arrays.sort(prioritized, new Comparator<KeyManagerService>() {
                @Override
                public int compare(KeyManagerService o1, KeyManagerService o2) {
                    return o1.getPriority() - o2.getPriority();
                }
            });
            final Map<Class<?>, KeyManager<?>> sorted = new TreeMap<>(
                    new Comparator<Class<?>>() {
                        @Override
                        public int compare(Class<?> o1, Class<?> o2) {
                            return o1.getName().compareTo(o2.getName());
                        }
                    });
            for (final KeyManagerService service : prioritized) {
                for (final Map.Entry<Class<?>, KeyManager<?>> entry
                        : service.getKeyManagers().entrySet()) {
                    final Class<?> type = entry.getKey();
                    final KeyManager<?> manager = entry.getValue();
                    if (null != type)
                        if (null != manager)
                            sorted.put(type, manager);
                        else
                            sorted.remove(type);
                }
            }
            final Map<Class<?>, KeyManager<?>> fast
                    = new LinkedHashMap<>(initialCapacity(sorted.size()));
            for (final Map.Entry<Class<?>, KeyManager<?>> entry : sorted.entrySet()) {
                final Class<?> type = entry.getKey();
                final KeyManager<?> manager = entry.getValue();
                logger.log(CONFIG, "mapping", new Object[] { type, manager });
                fast.put(type, manager);
            }
            MANAGERS = Collections.unmodifiableMap(fast);
        }
    } // Boot
}
