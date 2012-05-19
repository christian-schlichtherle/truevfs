/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.truezip.kernel.sl;

import de.truezip.kernel.FsManager;
import de.truezip.kernel.FsManagerProvider;
import de.truezip.kernel.spi.FsManagerService;
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
 * Locates a file system manager service of a class with a name which is
 * resolved by querying a system property or searching the class path
 * or using a default implementation, whatever yields a result first.
 * <p>
 * First, the value of the {@link System#getProperty system property}
 * with the class name {@code "de.truezip.kernel.spi.FsManagerService"}
 * as the key is queried.
 * If this yields a value, the class with that name is then loaded and
 * instantiated by calling its public no-argument constructor.
 * <p>
 * Otherwise, the class path is searched for any resource file with the name
 * {@code "META-INF/services/de.truezip.kernel.spi.FsManagerService"}.
 * If this yields a result, the class with the name in this file is then loaded
 * and instantiated by calling its public no-argument constructor.
 * <p>
 * Otherwise, a {@link ServiceConfigurationError} gets thrown.
 *
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
    public FsManager getManager() {
        return Boot.SERVICE.getManager();
    }

    /** A static data utility class used for lazy initialization. */
    private static final class Boot {
        static final FsManagerService SERVICE;
        static {
            final Logger logger = Logger.getLogger(
                    FsManagerLocator.class.getName(),
                    FsManagerLocator.class.getName());
            final ServiceLocator locator = new ServiceLocator(
                    FsManagerLocator.class.getClassLoader());
            FsManagerService
                    service = locator.getService(FsManagerService.class, null);
            if (null == service) {
                FsManagerService newService = null;
                for (   final Iterator<FsManagerService>
                            i = locator.getServices(FsManagerService.class);
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
                                .getBundle(FsManagerLocator.class.getName())
                                .getString("null"),
                            FsManagerLocator.class));
            logger.log(CONFIG, "provided", service);
            SERVICE = service;
        }
    } // Boot
}
