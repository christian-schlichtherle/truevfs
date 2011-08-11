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

import edu.umd.cs.findbugs.annotations.DefaultAnnotation;
import edu.umd.cs.findbugs.annotations.NonNull;
import java.util.Map;
import java.util.ServiceConfigurationError;

/**
 * An abstract key manager provider.
 * <p>
 * Implementations must be thread-safe.
 *
 * @author  Christian Schlichtherle
 * @version $Id$
 */
@DefaultAnnotation(NonNull.class)
public abstract class AbstractKeyManagerProvider implements KeyManagerProvider {

    @Override
    @SuppressWarnings("unchecked")
    public <K> KeyManager<K> get(Class<K> type) {
        final Map<Class<?>, KeyManager<?>> map = get();
        final KeyManager<?> manager = map.get(type);
        if (null == manager)
            throw new ServiceConfigurationError("No key manager available for " + type);
        return (KeyManager<K>) manager;
    }

    /**
     * Returns an unmodifiable map of secret key classes to key managers.
     * Neither the keys nor the values of the returned map may be {@code null}
     * and subsequent calls must return a map which compares at least
     * {@link Map#equals(Object) equal} with the previously returned map.
     * 
     * @return an unmodifiable map of secret key classes to key managers.
     * @since  TrueZIP 7.2
     */
    public abstract Map<Class<?>, KeyManager<?>> get();
}
