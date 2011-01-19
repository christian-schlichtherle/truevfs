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
package de.schlichtherle.truezip.socket;

import de.schlichtherle.truezip.util.ServiceLocator;
import java.util.Iterator;
import java.util.ServiceConfigurationError;
import net.jcip.annotations.Immutable;

/**
 * Contains an I/O pool service of a class with a name which is resolved by
 * querying a system property or searching the class path, whatever yields a
 * result first.
 * <p>
 * First, the value of the {@link System#getProperty system property}
 * with the class name {@code "de.schlichtherle.truezip.socket.IOPoolService"}
 * as the key is queried.
 * If this yields a value, the class with that name is then loaded and
 * instantiated by calling its no-arg constructor.
 * <p>
 * Otherwise, the class path is searched for any resource file with the name
 * {@code "META-INF/services/de.schlichtherle.truezip.socket.IOPoolService"}.
 * If this yields a result, the class with the name in this file is then loaded
 * and instantiated by calling its no-arg constructor.
 * <p>
 * Otherwise, a {@link ServiceConfigurationError} is thrown.
 * <p>
 * Note that the kernel classes have no dependency on this class; so using
 * this service locator is completely optional for a pure kernel application.
 *
 * @author Christian Schlichtherle
 * @version $Id$
 */
@Immutable
public final class IOPoolContainer implements IOPoolService {

    /** The singleton instance of this class. */
    public static final IOPoolContainer SINGLETON = new IOPoolContainer();

    private final IOPoolService service;

    /** You cannot instantiate this class. */
    private IOPoolContainer() {
        final ServiceLocator locator = new ServiceLocator(
                IOPoolContainer.class.getClassLoader());
        final IOPoolService
                service = locator.getService(IOPoolService.class, null);
        if (null != service) {
            this.service = service;
        } else {
            final Iterator<IOPoolService>
                    i = locator.getServices(IOPoolService.class);
            if (i.hasNext())
                this.service = i.next();
            else
                throw new ServiceConfigurationError(
                        "No service available for " + IOPoolService.class);
        }
    }

    /**
     * {@inheritDoc}
     * <p>
     * The implementation in the class {@link IOPoolContainer} delegates the
     * call to the container loaded by the constructor.
     */
    @Override
    public IOPool<?> getPool() {
        return service.getPool();
    }
}
