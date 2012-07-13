/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.truevfs.kernel.spec.sl;

import java.text.MessageFormat;
import java.util.*;
import static java.util.logging.Level.CONFIG;
import static java.util.logging.Level.WARNING;
import java.util.logging.Logger;
import javax.annotation.concurrent.Immutable;
import net.truevfs.kernel.spec.FsManager;
import net.truevfs.kernel.spec.FsManagerProvider;
import net.truevfs.kernel.spec.spi.FsManagerDecorator;
import net.truevfs.kernel.spec.spi.FsManagerFactory;
import net.truevfs.kernel.spec.util.ServiceLocator;

/**
 * Locates a file system manager service of a class with a name which is
 * resolved by querying a system property or searching the class path
 * or using a default implementation, whatever yields a result first.
 * <p>
 * First, the value of the {@link System#getProperty system property}
 * with the class name {@code "net.truevfs.kernel.spi.FsManagerFactory"}
 * as the key is queried.
 * If this yields a value, the class with that name is then loaded and
 * instantiated by calling its public no-argument constructor.
 * <p>
 * Otherwise, the class path is searched for any resource file with the name
 * {@code "META-INF/services/net.truevfs.kernel.spi.FsManagerFactory"}.
 * If this yields a result, the class with the name in this file is then loaded
 * and instantiated by calling its public no-argument constructor.
 * <p>
 * Otherwise, a {@link ServiceConfigurationError} gets thrown.
 *
 * @see    FsManagerFactory
 * @author Christian Schlichtherle
 */
@Immutable
public final class FsManagerLocator implements FsManagerProvider {

    /** The singleton instance of this class. */
    public static final FsManagerLocator SINGLETON = new FsManagerLocator();

    /** Can't touch this - hammer time! */
    private FsManagerLocator() { }

    @Override
    public FsManager manager() {
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
            FsManagerFactory factory
                    = locator.getService(FsManagerFactory.class, null);
            if (null == factory) {
                for (final Iterator<FsManagerFactory> i = locator.getServices(FsManagerFactory.class);
                        i.hasNext(); ) {
                    final FsManagerFactory newFactory = i.next();
                    logger.log(CONFIG, "located", newFactory);
                    if (null == factory) {
                        factory = newFactory;
                    } else {
                        final int op = factory.getPriority();
                        final int np = newFactory.getPriority();
                        if (op < np)
                            factory = newFactory;
                        else if (op == np)
                            logger.log(WARNING, "collision",
                                    new Object[] { op, factory, newFactory });
                    }
                }
            }
            if (null == factory)
                throw new ServiceConfigurationError(
                        MessageFormat.format(
                            ResourceBundle
                                .getBundle(FsManagerLocator.class.getName())
                                .getString("null"),
                            FsManagerLocator.class));
            logger.log(CONFIG, "creating", factory);
            return factory.manager();
        }

        private static FsManager decorate(
                FsManager manager,
                final ServiceLocator locator,
                final Logger logger) {
            final List<FsManagerDecorator> list = new ArrayList<>();
            for (final Iterator<FsManagerDecorator> i = locator.getServices(FsManagerDecorator.class);
                    i.hasNext(); ) {
                list.add(i.next());
            }
            final FsManagerDecorator[] array = list.toArray(new FsManagerDecorator[list.size()]);
            Arrays.sort(array, new ServiceProviderComparator());
            for (final FsManagerDecorator decorator : array) {
                logger.log(CONFIG, "decorating", decorator);
                manager = decorator.decorate(manager);
            }
            return manager;
        }
    } // Boot
}
