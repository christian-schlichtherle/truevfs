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
import de.schlichtherle.truezip.socket.IOPoolService;
import de.schlichtherle.truezip.socket.spi.IOPoolProvider;
import de.schlichtherle.truezip.util.ServiceLocator;
import java.util.Iterator;
import java.util.ServiceConfigurationError;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.jcip.annotations.Immutable;

/**
 * Locates an I/O pool service of a class with a name which is
 * resolved by querying a system property or searching the class path,
 * whatever yields a result first.
 * <p>
 * First, the value of the {@link System#getProperty system property}
 * with the class name {@code "de.schlichtherle.truezip.socket.spi.IOPoolProvider"}
 * as the key is queried.
 * If this yields a value, the class with that name is then loaded and
 * instantiated by calling its no-arg constructor.
 * <p>
 * Otherwise, the class path is searched for any resource file with the name
 * {@code "META-INF/services/de.schlichtherle.truezip.socket.spi.IOPoolProvider"}.
 * If this yields a result, the class with the name in this file is then loaded
 * and instantiated by calling its no-arg constructor.
 * <p>
 * Otherwise, a {@link ServiceConfigurationError} is thrown.
 *
 * @author Christian Schlichtherle
 * @version $Id$
 */
@Immutable
public final class IOPoolLocator implements IOPoolService {

    /** The singleton instance of this class. */
    public static final IOPoolLocator SINGLETON = new IOPoolLocator();

    private final IOPoolProvider provider;

    /** You cannot instantiate this class. */
    private IOPoolLocator() {
        final ServiceLocator locator = new ServiceLocator(
                IOPoolLocator.class.getClassLoader());
        IOPoolProvider
                provider = locator.getService(IOPoolProvider.class, null);
        if (null == provider) {
            final Iterator<IOPoolProvider>
                    i = locator.getServices(IOPoolProvider.class);
            if (i.hasNext())
                provider = i.next();
            else
                throw new ServiceConfigurationError(
                        "No service provider available for " + IOPoolProvider.class);
        }
        this.provider = provider;
        Logger  .getLogger( IOPoolLocator.class.getName(),
                            IOPoolLocator.class.getName())
                .log(Level.CONFIG, "located", provider);
    }

    /**
     * {@inheritDoc}
     * <p>
     * The implementation in the class {@link IOPoolLocator} delegates the
     * call to the container loaded by the constructor.
     */
    @Override
    public IOPool<?> getPool() {
        return provider.getPool();
    }
}
