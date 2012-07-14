/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.fs.sl;

import de.schlichtherle.truezip.fs.FsDefaultManager;
import de.schlichtherle.truezip.fs.FsManager;
import de.schlichtherle.truezip.fs.FsManagerProvider;
import de.schlichtherle.truezip.fs.spi.FsManagerDecorator;
import de.schlichtherle.truezip.fs.spi.FsManagerService;
import de.schlichtherle.truezip.util.ServiceLocator;
import java.util.*;
import static java.util.logging.Level.CONFIG;
import static java.util.logging.Level.WARNING;
import java.util.logging.Logger;
import javax.annotation.concurrent.Immutable;

/**
 * Locates a file system manager service of a class with a name which is
 * resolved by querying a system property or searching the class path
 * or using a default implementation, whatever yields a result first.
 * <p>
 * First, the value of the {@link System#getProperty system property}
 * with the class name {@code "de.schlichtherle.truezip.fs.spi.FsManagerService"}
 * as the key is queried.
 * If this yields a value, the class with that name is then loaded and
 * instantiated by calling its public no-argument constructor.
 * <p>
 * Otherwise, the class path is searched for any resource file with the name
 * {@code "META-INF/services/de.schlichtherle.truezip.fs.spi.FsManagerService"}.
 * If this yields a result, the class with the name in this file is then loaded
 * and instantiated by calling its public no-argument constructor.
 * <p>
 * Otherwise, the expression
 * {@code new FsDefaultManager()} is used to create the file system manager in
 * this container.
 *
 * @see    FsDefaultManager
 * @see    FsManagerService
 * @author Christian Schlichtherle
 */
@Immutable
public final class FsManagerLocator implements FsManagerProvider {

    /** The singleton instance of this class. */
    public static final FsManagerLocator SINGLETON = new FsManagerLocator();

    /** Can't touch this - hammer time! */
    private FsManagerLocator() { }

    @Override
    public FsManager get() {
        return Boot.manager;
    }

    /** A static data utility class used for lazy initialization. */
    private static final class Boot {
        static final FsManager manager;
        static {
            final Class<?> clazz = FsManagerLocator.class;
            final Logger logger = Logger.getLogger(
                    clazz.getName(), clazz.getName());
            final ServiceLocator locator = new ServiceLocator(
                    clazz.getClassLoader());
            manager = decorate(create(locator, logger), locator, logger);
        }

        private static FsManager create(
                final ServiceLocator locator,
                final Logger logger) {
            FsManagerService service
                    = locator.getService(FsManagerService.class, null);
            if (null == service) {
                for (final Iterator<FsManagerService> i = locator.getServices(FsManagerService.class);
                        i.hasNext(); ) {
                    final FsManagerService newService = i.next();
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
            if (null == service) service = new DefaultManagerService();
            logger.log(CONFIG, "using", service);
            final FsManager manager = service.get();
            logger.log(CONFIG, "result", manager);
            return manager;
        }

        private static FsManager decorate(
                FsManager manager,
                final ServiceLocator locator,
                final Logger logger) {
            final List<FsManagerDecorator> list = new ArrayList<FsManagerDecorator>();
            for (final Iterator<FsManagerDecorator> i = locator.getServices(FsManagerDecorator.class);
                    i.hasNext(); ) {
                list.add(i.next());
            }
            final FsManagerDecorator[] array = list.toArray(new FsManagerDecorator[list.size()]);
            Arrays.sort(array, new Comparator<FsManagerDecorator>() {
                @Override
                public int compare(FsManagerDecorator o1, FsManagerDecorator o2) {
                    return o1.getPriority() - o2.getPriority();
                }
            });
            for (final FsManagerDecorator decorator : array) {
                logger.log(CONFIG, "decorating", decorator);
                manager = decorator.decorate(manager);
                logger.log(CONFIG, "result", manager);
            }
            return manager;
        }
    } // Boot

    private static final class DefaultManagerService extends FsManagerService {
        @Override
        public FsManager get() {
            return new FsDefaultManager();
        }
    } // DefaultManagerService
}
