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
package de.schlichtherle.truezip.key.sl;

import de.schlichtherle.truezip.key.KeyManager;
import de.schlichtherle.truezip.key.KeyManagerProvider;
import de.schlichtherle.truezip.key.spi.KeyManagerService;
import de.schlichtherle.truezip.util.ServiceLocator;
import edu.umd.cs.findbugs.annotations.DefaultAnnotation;
import edu.umd.cs.findbugs.annotations.NonNull;
import java.util.Iterator;
import java.util.ServiceConfigurationError;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.jcip.annotations.Immutable;

/**
 * Locates a key manager service of a class with a name which
 * is resolved by querying a system property or searching the class path,
 * whatever yields a result first.
 * <p>
 * First, the value of the {@link System#getProperty system property}
 * with the class name {@code "de.schlichtherle.truezip.key.spi.KeyManagerService"}
 * as the key is queried.
 * If this yields a value, the class with that name is then loaded and
 * instantiated by calling its no-arg constructor.
 * <p>
 * Otherwise, the class path is searched for any resource file with the name
 * {@code "META-INF/services/de.schlichtherle.truezip.key.spi.KeyManagerService"}.
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
public final class KeyManagerLocator implements KeyManagerProvider {

    /** The singleton instance of this class. */
    public static final KeyManagerLocator
            SINGLETON = new KeyManagerLocator();

    private final KeyManagerService service;

    /** You cannot instantiate this class. */
    private KeyManagerLocator() {
        final ServiceLocator locator = new ServiceLocator(
                KeyManagerLocator.class.getClassLoader());
        KeyManagerService
                service = locator.getService(KeyManagerService.class, null);
        if (null == service) {
            final Iterator<KeyManagerService>
                    i = locator.getServices(KeyManagerService.class);
            if (i.hasNext())
                service = i.next();
            else
                throw new ServiceConfigurationError(
                        "No provider available for " + KeyManagerService.class);
        }
        this.service = service;
        Logger  .getLogger( KeyManagerLocator.class.getName(),
                            KeyManagerLocator.class.getName())
                .log(Level.CONFIG, "located", service);
    }

    @Override
    public <K> KeyManager<? extends K, ?> get(Class<K> type) {
        return service.get(type);
    }
}
