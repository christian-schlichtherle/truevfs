/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.truevfs.kernel.sl;

import net.truevfs.kernel.cio.IoPool;
import net.truevfs.kernel.cio.IoPoolProvider;
import net.truevfs.kernel.spi.IoPoolService;
import net.truevfs.kernel.util.ServiceLocator;
import java.text.MessageFormat;
import java.util.Iterator;
import java.util.ResourceBundle;
import java.util.ServiceConfigurationError;
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
 * with the class name {@code "net.truevfs.kernel.spi.IoPoolService"}
 * as the key is queried.
 * If this yields a value, the class with that name is then loaded and
 * instantiated by calling its public no-argument constructor.
 * <p>
 * Otherwise, the class path is searched for any resource file with the name
 * {@code "META-INF/services/net.truevfs.kernel.spi.IoPoolService"}.
 * If this yields a result, the class with the name in this file is then loaded
 * and instantiated by calling its public no-argument constructor.
 * <p>
 * Otherwise, a {@link ServiceConfigurationError} gets thrown.
 *
 * @see    IoPoolService
 * @author Christian Schlichtherle
 */
@Immutable
public final class IoPoolLocator implements IoPoolProvider {

    /** The singleton instance of this class. */
    public static final IoPoolLocator SINGLETON = new IoPoolLocator();

    /** Can't touch this - hammer time! */
    private IoPoolLocator() { }

    @Override
    public IoPool<?> getIoPool() {
        return Boot.SERVICE.getIoPool();
    }

    /** A static data utility class used for lazy initialization. */
    private static final class Boot {
        static final IoPoolService SERVICE;
        static {
            final Logger logger = Logger.getLogger(
                    IoPoolLocator.class.getName(),
                    IoPoolLocator.class.getName());
            final ServiceLocator locator = new ServiceLocator(
                    IoPoolLocator.class.getClassLoader());
            IoPoolService service = locator.getService(IoPoolService.class, null);
            if (null == service) {
                IoPoolService newService = null;
                for (   final Iterator<IoPoolService>
                            i = locator.getServices(IoPoolService.class);
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
                            IoPoolService.class));
            logger.log(CONFIG, "provided", service);
            SERVICE = service;
        }
    } // Boot
}