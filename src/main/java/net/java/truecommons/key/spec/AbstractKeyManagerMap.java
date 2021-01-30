/*
 * Copyright (C) 2005-2015 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truecommons.key.spec;

import net.java.truecommons.services.Container;
import net.java.truecommons.shed.UniqueObject;

import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import java.util.Map;
import java.util.ServiceConfigurationError;

/**
 * An abstract key manager map.
 * When implementing a key manager map, you should extend this class rather
 * than directly implementing the interface in order to maintain binary
 * backwards compatibility even if the interface is changed.
 * <p>
 * Implementations must be safe for multi-threading.
 *
 * @since TrueCommons 2.2
 * @author Christian Schlichtherle
 */
@Immutable
public abstract class AbstractKeyManagerMap
extends UniqueObject
implements KeyManagerMap, Container<Map<Class<?>, KeyManager<?>>> {

    @Override
    @SuppressWarnings("unchecked")
    public final @Nullable <K> KeyManager<K> manager(final Class<K> type) {
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

    /**
     * Returns a string representation of this object for logging and debugging
     * purposes.
     */
    @Override
    public String toString() {
        return String.format("%s[keyManagers=%s]",
                getClass().getName(),
                get());
    }
}
