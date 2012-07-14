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
import net.truevfs.kernel.spec.cio.IoBuffer;
import net.truevfs.kernel.spec.cio.IoBufferPool;
import net.truevfs.kernel.spec.cio.IoBufferPoolProvider;
import net.truevfs.kernel.spec.spi.IoBufferPoolDecorator;
import net.truevfs.kernel.spec.spi.IoBufferPoolFactory;
import net.truevfs.kernel.spec.util.ServiceLocator;

/**
 * Locates an I/O buffer pool service of a class with a name which is
 * resolved by querying a system property or searching the class path,
 * whatever yields a result first.
 * <p>
 * First, the value of the {@link System#getProperty system property}
 * with the class name {@code "net.truevfs.kernel.spi.IoBufferPoolFactory"}
 * as the key is queried.
 * If this yields a value, the class with that name is then loaded and
 * instantiated by calling its public no-argument constructor.
 * <p>
 * Otherwise, the class path is searched for any resource file with the name
 * {@code "META-INF/services/net.truevfs.kernel.spi.IoBufferPoolFactory"}.
 * If this yields a result, the class with the name in this file is then loaded
 * and instantiated by calling its public no-argument constructor.
 * <p>
 * Otherwise, a {@link ServiceConfigurationError} gets thrown.
 *
 * @see    IoBufferPoolFactory
 * @author Christian Schlichtherle
 */
@Immutable
public final class IoBufferPoolLocator implements IoBufferPoolProvider {

    /** The singleton instance of this class. */
    public static final IoBufferPoolLocator SINGLETON = new IoBufferPoolLocator();

    /** Can't touch this - hammer time! */
    private IoBufferPoolLocator() { }

    @Override
    public IoBufferPool<? extends IoBuffer<?>> pool() {
        return Boot.POOL;
    }

    /** A static data utility class used for lazy initialization. */
    private static final class Boot {
        static final IoBufferPool<? extends IoBuffer<?>> POOL = new Boot().create().decorate().result;

        final Class<IoBufferPoolLocator> locatorClass = IoBufferPoolLocator.class;
        final Class<IoBufferPoolFactory> factoryClass = IoBufferPoolFactory.class;
        final Class<IoBufferPoolDecorator> decoratorClass = IoBufferPoolDecorator.class;
        final ResourceBundle bundle = ResourceBundle.getBundle(
                                                locatorClass.getName());
        final Logger logger = Logger.getLogger( locatorClass.getName(),
                                                locatorClass.getName());
        final ServiceLocator locator = new ServiceLocator(
                locatorClass.getClassLoader());
        IoBufferPool<? extends IoBuffer<?>> result;

        private Boot create() {
            IoBufferPoolFactory factory = locator.getService(factoryClass, null);
            if (null == factory) {
                for (final Iterator<IoBufferPoolFactory> i = locator.getServices(factoryClass);
                        i.hasNext();) {
                    final IoBufferPoolFactory newFactory = i.next();
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
                        MessageFormat.format(   bundle.getString("null"),
                                                factoryClass));
            logger.log(CONFIG, "creating", factory);
            result = factory.pool();
            logger.log(CONFIG, "result", result);
            return this;
        }

        private Boot decorate() {
            final List<IoBufferPoolDecorator> list = new ArrayList<>();
            for (final Iterator<IoBufferPoolDecorator> i = locator.getServices(IoBufferPoolDecorator.class);
                    i.hasNext(); ) {
                list.add(i.next());
            }
            final IoBufferPoolDecorator[] array = list.toArray(new IoBufferPoolDecorator[list.size()]);
            Arrays.sort(array, new ServiceProviderComparator());
            for (final IoBufferPoolDecorator decorator : array) {
                logger.log(CONFIG, "decorating", decorator);
                result = decorator.decorate(result);
                logger.log(CONFIG, "result", result);
            }
            return this;
        }
    } // Boot
}
