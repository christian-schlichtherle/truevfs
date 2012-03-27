/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.truezip.key.spi;

import de.truezip.kernel.util.Maps;
import de.truezip.kernel.util.ServiceLocator;
import de.truezip.key.AbstractKeyManagerProvider;
import de.truezip.key.KeyManager;
import de.truezip.key.sl.KeyManagerLocator;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * An abstract locatable service for key managers.
 * Implementations of this abstract class are subject to service location
 * by the class {@link KeyManagerLocator}.
 * <p>
 * Implementations must be thread-safe.
 *
 * @author Christian Schlichtherle
 */
public abstract class KeyManagerService extends AbstractKeyManagerProvider {

    /**
     * Returns a string representation of this object for debugging and logging
     * purposes.
     */
    @Override
    public String toString() {
        return getClass().getName();
    }

    /**
     * A static factory method for an unmodifiable key manager map which is
     * constructed from the given configuration.
     * This method is intended to be used by implementations of this class
     * for convenient creation of the map to return by their {@link #get()}
     * method.
     *
     * @param  config an array of key-value pair arrays.
     *         The first element of each inner array must either be a
     *         {@link Class secret key class}, a
     *         {@link String fully qualified name of a secret key class},
     *         or {@code null}.
     *         The second element of each inner array must either be a
     *         {@link KeyManager key manager instance}, a
     *         {@link Class key manager class}, a
     *         {@link String fully qualified name of a key manager class},
     *         or {@code null}.
     * @return The new map to use as the return value of {@link #get()}.
     * @throws NullPointerException if a required configuration element is
     *         {@code null}.
     * @throws IllegalArgumentException if any other parameter precondition
     *         does not hold.
     */
    public static Map<Class<?>, KeyManager<?>> newMap(final Object[][] config) {
        final Map<Class<?>, KeyManager<?>>
                managers = new HashMap<Class<?>, KeyManager<?>>(
                    Maps.initialCapacity(config.length));
        for (final Object[] param : config) {
            final Class<?> type = ServiceLocator.promote(param[0], Class.class);
            final KeyManager<?> newManager = ServiceLocator
                    .promote(param[1], KeyManager.class);
            final KeyManager<?> oldManager = managers.put(type, newManager);
            if (null != oldManager && null != newManager
                    && oldManager.getPriority() > newManager.getPriority())
                managers.put(type, oldManager);
        }
        return Collections.unmodifiableMap(managers);
    }
}
