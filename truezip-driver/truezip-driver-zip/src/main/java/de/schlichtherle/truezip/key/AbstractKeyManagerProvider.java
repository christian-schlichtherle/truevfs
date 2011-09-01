/*
 * Copyright (C) 2011 Schlichtherle IT Services
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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
