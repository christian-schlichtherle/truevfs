/*
 * Copyright (C) 2011 Schlichtherle IT Services
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.schlichtherle.truezip.socket.sl;

import de.schlichtherle.truezip.socket.IOPool;
import de.schlichtherle.truezip.socket.IOPoolProvider;
import de.schlichtherle.truezip.socket.spi.IOPoolService;
import de.schlichtherle.truezip.util.ServiceLocator;
import edu.umd.cs.findbugs.annotations.DefaultAnnotation;
import edu.umd.cs.findbugs.annotations.NonNull;
import java.text.MessageFormat;
import java.util.Iterator;
import java.util.ResourceBundle;
import java.util.ServiceConfigurationError;
import static java.util.logging.Level.*;
import java.util.logging.Logger;
import net.jcip.annotations.Immutable;

/**
 * Locates an I/O pool service of a class with a name which is
 * resolved by querying a system property or searching the class path,
 * whatever yields a result first.
 * <p>
 * First, the value of the {@link System#getProperty system property}
 * with the class name {@code "de.schlichtherle.truezip.socket.spi.IOPoolService"}
 * as the key is queried.
 * If this yields a value, the class with that name is then loaded and
 * instantiated by calling its no-arg constructor.
 * <p>
 * Otherwise, the class path is searched for any resource file with the name
 * {@code "META-INF/services/de.schlichtherle.truezip.socket.spi.IOPoolService"}.
 * If this yields a result, the class with the name in this file is then loaded
 * and instantiated by calling its no-arg constructor.
 * <p>
 * Otherwise, a {@link ServiceConfigurationError} is thrown.
 *
 * @author Christian Schlichtherle
 * @version $Id$
 */
@Immutable
@DefaultAnnotation(NonNull.class)
public final class IOPoolLocator implements IOPoolProvider {

    /** The singleton instance of this class. */
    public static final IOPoolLocator SINGLETON = new IOPoolLocator();

    /** You cannot instantiate this class. */
    private IOPoolLocator() {
    }

    /**
     * {@inheritDoc}
     * <p>
     * The implementation in the class {@link IOPoolLocator} delegates the
     * call to the container loaded by the constructor.
     */
    @Override
    public IOPool<?> get() {
        return Boot.SERVICE.get();
    }

    /** A static data utility class used for lazy initialization. */
    private static class Boot {
        static final IOPoolService SERVICE;
        static {
            final Logger logger = Logger.getLogger(
                    IOPoolLocator.class.getName(),
                    IOPoolLocator.class.getName());
            final ServiceLocator locator = new ServiceLocator(
                    IOPoolLocator.class.getClassLoader());
            IOPoolService service = locator.getService(IOPoolService.class, null);
            if (null == service) {
                IOPoolService oldService = null;
                for (   final Iterator<IOPoolService>
                            i = locator.getServices(IOPoolService.class);
                        i.hasNext();
                        oldService = service) {
                    service = i.next();
                    logger.log(CONFIG, "located", service);
                    if (null != oldService
                            && oldService.getPriority() > service.getPriority())
                        service = oldService;
                }
            }
            if (null != service)
                logger.log(CONFIG, "provided", service);
            else
                throw new ServiceConfigurationError(
                        MessageFormat.format(
                            ResourceBundle
                                .getBundle(IOPoolLocator.class.getName())
                                .getString("none"),
                            IOPoolService.class));
            SERVICE = service;
        }
    } // class Boot
}
