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
package de.schlichtherle.truezip.key;

import de.schlichtherle.truezip.util.ServiceLocator;
import edu.umd.cs.findbugs.annotations.DefaultAnnotation;
import edu.umd.cs.findbugs.annotations.NonNull;
import java.util.Iterator;
import java.util.ServiceConfigurationError;
import net.jcip.annotations.Immutable;

/**
 * Contains a key manager of a class with a name which is resolved by
 * querying a system property or searching the class path, whatever yields a
 * result first.
 * <p>
 * First, the value of the {@link System#getProperty system property}
 * with the class name {@code "de.schlichtherle.truezip.key.KeyManagerService"}
 * as the key is queried.
 * If this yields a value, the class with that name is then loaded and
 * instantiated by calling its no-arg constructor.
 * <p>
 * Otherwise, the class path is searched for any resource file with the name
 * {@code "META-INF/services/de.schlichtherle.truezip.key.KeyManagerService"}.
 * If this yields a result, the class with the name in this file is then loaded
 * and instantiated by calling its no-arg constructor.
 * <p>
 * Otherwise, a {@link ServiceConfigurationError} is thrown.
 * <p>
 * Note that the driver classes have no dependency on this class; so using
 * this service locator is completely optional.
 *
 * @author Christian Schlichtherle
 * @version $Id$
 */
@Immutable
@DefaultAnnotation(NonNull.class)
public final class KeyManagerContainer implements KeyManagerService {

    /** The singleton instance of this class. */
    public static final KeyManagerContainer
            SINGLETON = new KeyManagerContainer();

    private final KeyManagerService service;

    /** You cannot instantiate this class. */
    private KeyManagerContainer() {
        final ServiceLocator locator = new ServiceLocator(
                KeyManagerContainer.class.getClassLoader());
        final KeyManagerService
                service = locator.getService(KeyManagerService.class, null);
        if (null != service) {
            this.service = service;
        } else {
            final Iterator<KeyManagerService>
                    i = locator.getServices(KeyManagerService.class);
            if (i.hasNext())
                this.service = i.next();
            else
                throw new ServiceConfigurationError(
                        "No service available for " + KeyManagerService.class);
        }
    }

    @Override
    public <K> KeyManager<? extends K, ?> getManager(Class<K> type) {
        return service.getManager(type);
    }
}
