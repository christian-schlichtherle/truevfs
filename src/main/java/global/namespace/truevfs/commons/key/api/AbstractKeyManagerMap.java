/*
 * Copyright (C) 2005-2015 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package global.namespace.truevfs.commons.key.api;

import java.util.Map;
import java.util.ServiceConfigurationError;
import java.util.function.Supplier;

/**
 * An abstract key manager map.
 * <p>
 * Subclasses must be thread-safe.
 *
 * @author Christian Schlichtherle
 */
public abstract class AbstractKeyManagerMap implements KeyManagerMap, Supplier<Map<Class<?>, KeyManager<?>>> {

    @Override
    @SuppressWarnings("unchecked")
    public final <K> KeyManager<K> manager(final Class<K> type) {
        final KeyManager<?> m = get().get(type);
        if (null == m)
            throw new ServiceConfigurationError("No key manager available for " + type + ".");
        return (KeyManager<K>) m;
    }

    /**
     * Returns an immutable map of key classes to nullable key managers.
     * Only the values of the returned map may be {@code null}.
     * <p>
     * This is an immutable property - multiple calls must return the same
     * object.
     *
     * @return An immutable map of key classes to nullable key managers.
     */
    @Override
    public abstract Map<Class<?>, KeyManager<?>> get();
}
