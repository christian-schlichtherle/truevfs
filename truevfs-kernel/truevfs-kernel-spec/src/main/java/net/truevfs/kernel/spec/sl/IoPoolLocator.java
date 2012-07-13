/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.truevfs.kernel.spec.sl;

import java.text.MessageFormat;
import java.util.Iterator;
import java.util.ResourceBundle;
import java.util.ServiceConfigurationError;
import static java.util.logging.Level.CONFIG;
import static java.util.logging.Level.WARNING;
import java.util.logging.Logger;
import javax.annotation.concurrent.Immutable;
import net.truevfs.kernel.spec.cio.IoBuffer;
import net.truevfs.kernel.spec.cio.IoBufferPool;
import net.truevfs.kernel.spec.cio.IoBufferPoolProvider;
import net.truevfs.kernel.spec.spi.IoPoolFactory;
import net.truevfs.kernel.spec.util.ServiceLocator;

/**
 * Locates an I/O buffer pool service of a class with a name which is
 * resolved by querying a system property or searching the class path,
 * whatever yields a result first.
 * <p>
 * First, the value of the {@link System#getProperty system property}
 * with the class name {@code "net.truevfs.kernel.spi.IoPoolFactory"}
 * as the key is queried.
 * If this yields a value, the class with that name is then loaded and
 * instantiated by calling its public no-argument constructor.
 * <p>
 * Otherwise, the class path is searched for any resource file with the name
 * {@code "META-INF/services/net.truevfs.kernel.spi.IoPoolFactory"}.
 * If this yields a result, the class with the name in this file is then loaded
 * and instantiated by calling its public no-argument constructor.
 * <p>
 * Otherwise, a {@link ServiceConfigurationError} gets thrown.
 *
 * @see    IoPoolFactory
 * @author Christian Schlichtherle
 */
@Immutable
public final class IoPoolLocator implements IoBufferPoolProvider {

    /** The singleton instance of this class. */
    public static final IoPoolLocator SINGLETON = new IoPoolLocator();

    /** Can't touch this - hammer time! */
    private IoPoolLocator() { }

    @Override
    public IoBufferPool<? extends IoBuffer<?>> ioBufferPool() {
        return Boot.SERVICE.ioBufferPool();
    }

    /** A static data utility class used for lazy initialization. */
    private static final class Boot {
        static final IoPoolFactory SERVICE;
        static {
            final Logger logger = Logger.getLogger(
                    IoPoolLocator.class.getName(),
                    IoPoolLocator.class.getName());
            final ServiceLocator locator = new ServiceLocator(
                    IoPoolLocator.class.getClassLoader());
            IoPoolFactory service = locator.getService(IoPoolFactory.class, null);
            if (null == service) {
                IoPoolFactory newService = null;
                for (   final Iterator<IoPoolFactory>
                            i = locator.getServices(IoPoolFactory.class);
                        i.hasNext();) {
                    newService = i.next();
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
                                .getBundle(IoPoolLocator.class.getName())
                                .getString("null"),
                            IoPoolFactory.class));
            logger.log(CONFIG, "provided", service);
            SERVICE = service;
        }
    } // Boot
}
