/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.truezip.kernel.sl;

import de.truezip.kernel.cio.IOPool;
import de.truezip.kernel.cio.IOPoolProvider;
import de.truezip.kernel.spi.IOPoolService;
import de.truezip.kernel.util.ServiceLocator;
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
 * with the class name {@code "de.truezip.kernel.spi.IOPoolService"}
 * as the key is queried.
 * If this yields a value, the class with that name is then loaded and
 * instantiated by calling its public no-argument constructor.
 * <p>
 * Otherwise, the class path is searched for any resource file with the name
 * {@code "META-INF/services/de.truezip.kernel.spi.IOPoolService"}.
 * If this yields a result, the class with the name in this file is then loaded
 * and instantiated by calling its public no-argument constructor.
 * <p>
 * Otherwise, a {@link ServiceConfigurationError} gets thrown.
 *
 * @see    IOPoolService
 * @author Christian Schlichtherle
 */
@Immutable
public final class IOPoolLocator implements IOPoolProvider {

    /** The singleton instance of this class. */
    public static final IOPoolLocator SINGLETON = new IOPoolLocator();

    /** You cannot instantiate this class. */
    private IOPoolLocator() {
    }

    @Override
    public IOPool<?> get() {
        return Boot.SERVICE.get();
    }

    /** A static data utility class used for lazy initialization. */
    private static final class Boot {
        static final IOPoolService SERVICE;
        static {
            final Logger logger = Logger.getLogger(
                    IOPoolLocator.class.getName(),
                    IOPoolLocator.class.getName());
            final ServiceLocator locator = new ServiceLocator(
                    IOPoolLocator.class.getClassLoader());
            IOPoolService service = locator.getService(IOPoolService.class, null);
            if (null == service) {
                IOPoolService newService = null;
                for (   final Iterator<IOPoolService>
                            i = locator.getServices(IOPoolService.class);
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
                                .getBundle(IOPoolLocator.class.getName())
                                .getString("null"),
                            IOPoolService.class));
            logger.log(CONFIG, "provided", service);
            SERVICE = service;
        }
    } // Boot
}