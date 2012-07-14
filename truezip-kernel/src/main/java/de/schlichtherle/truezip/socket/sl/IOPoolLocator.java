/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.socket.sl;

import de.schlichtherle.truezip.socket.IOPool;
import de.schlichtherle.truezip.socket.IOPoolProvider;
import de.schlichtherle.truezip.socket.spi.IOPoolDecorator;
import de.schlichtherle.truezip.socket.spi.IOPoolService;
import de.schlichtherle.truezip.util.ServiceLocator;
import java.text.MessageFormat;
import java.util.*;
import static java.util.logging.Level.CONFIG;
import static java.util.logging.Level.WARNING;
import java.util.logging.Logger;
import javax.annotation.concurrent.Immutable;

/**
 * Locates an I/O buffer pool service of a class with a name which is
 * resolved by querying a system property or searching the class path,
 * whatever yields a result first.
 * <p>
 * First, the value of the {@link System#getProperty system property}
 * with the class name {@code "de.schlichtherle.truezip.socket.spi.IOPoolService"}
 * as the key is queried.
 * If this yields a value, the class with that name is then loaded and
 * instantiated by calling its public no-argument constructor.
 * <p>
 * Otherwise, the class path is searched for any resource file with the name
 * {@code "META-INF/services/de.schlichtherle.truezip.socket.spi.IOPoolService"}.
 * If this yields a result, the class with the name in this file is then loaded
 * and instantiated by calling its public no-argument constructor.
 * <p>
 * Otherwise, a {@link ServiceConfigurationError} is thrown.
 *
 * @see     IOPoolService
 * @author  Christian Schlichtherle
 */
@Immutable
public final class IOPoolLocator implements IOPoolProvider {

    /** The singleton instance of this class. */
    public static final IOPoolLocator SINGLETON = new IOPoolLocator();

    /** Can't touch this - hammer time! */
    private IOPoolLocator() { }

    @Override
    public IOPool<?> get() {
        return Boot.pool;
    }

    /** A static data utility class used for lazy initialization. */
    @SuppressWarnings("unchecked")
    private static final class Boot {
        static final IOPool<?> pool;
        static {
            final Class<?> clazz = IOPoolLocator.class;
            final Logger logger = Logger.getLogger(
                    clazz.getName(), clazz.getName());
            final ServiceLocator locator = new ServiceLocator(
                    clazz.getClassLoader());
            pool = decorate((IOPool) create(locator, logger), locator, logger);
        }

        private static IOPool<?> create(
                final ServiceLocator locator,
                final Logger logger) {
            IOPoolService service
                    = locator.getService(IOPoolService.class, null);
            if (null == service) {
                for (   final Iterator<IOPoolService>
                            i = locator.getServices(IOPoolService.class);
                        i.hasNext();) {
                    IOPoolService newService = i.next();
                    logger.log(CONFIG, "located", newService);
                    if (null == service) {
                        service = newService;
                    } else {
                        final int op = service.getPriority();
                        final int np = newService.getPriority();
                        if (op < np)
                            service = newService;
                        else if (op == np)
                            logger.log(WARNING, "collision",
                                    new Object[] { op, service, newService });
                    }
                }
            }
            if (null == service)
                throw new ServiceConfigurationError(
                        MessageFormat.format(
                            ResourceBundle
                                .getBundle(IOPoolLocator.class.getName())
                                .getString("null"),
                            IOPoolService.class));
            logger.log(CONFIG, "using", service);
            final IOPool<?> pool = service.get();
            logger.log(CONFIG, "result", pool);
            return pool;
        }

        private static <B extends IOPool.Entry<B>> IOPool<B> decorate(
                IOPool<B> pool,
                final ServiceLocator locator,
                final Logger logger) {
            final List<IOPoolDecorator> list = new ArrayList<IOPoolDecorator>();
            for (final Iterator<IOPoolDecorator> i = locator.getServices(IOPoolDecorator.class);
                    i.hasNext(); ) {
                list.add(i.next());
            }
            final IOPoolDecorator[] array = list.toArray(new IOPoolDecorator[list.size()]);
            Arrays.sort(array, new Comparator<IOPoolDecorator>() {
                @Override
                public int compare(IOPoolDecorator o1, IOPoolDecorator o2) {
                    return o1.getPriority() - o2.getPriority();
                }
            });
            for (final IOPoolDecorator decorator : array) {
                logger.log(CONFIG, "decorating", decorator);
                pool = decorator.decorate(pool);
                logger.log(CONFIG, "result", pool);
            }
            return pool;
        }
    } // Boot
}
