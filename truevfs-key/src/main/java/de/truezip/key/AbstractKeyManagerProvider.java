/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.truezip.key;

import static de.truezip.kernel.util.HashMaps.initialCapacity;
import de.truezip.kernel.util.ServiceLocator;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import javax.annotation.CheckForNull;
import javax.annotation.concurrent.Immutable;

/**
 * An abstract provider for an immutable map of secret key classes to nullable
 * key managers.
 *
 * @author Christian Schlichtherle
 */
@Immutable
public abstract class AbstractKeyManagerProvider implements KeyManagerProvider {

    /**
     * A static factory method for an unmodifiable key manager map which is
     * constructed from the given configuration.
     * This method is intended to be used by implementations of this class
     * for convenient creation of the map to return by their
     * {@link #getKeyManagers()} method.
     *
     * @param  config an array of key-value pair arrays.
     *         The first element of each inner array must either be a
     *         {@link Class secret key class}, or a
     *         {@link String fully qualified name of a secret key class}.
     *         The second element of each inner array must either be a
     *         {@link KeyManager key manager instance}, a
     *         {@link Class key manager class}, a
     *         {@link String fully qualified name of a key manager class},
     *         or {@code null}.
     * @return The new map to use as the return value of
     *         {@link #getKeyManagers()}.
     * @throws NullPointerException if a required configuration element is
     *         {@code null}.
     * @throws IllegalArgumentException if any other parameter precondition
     *         does not hold.
     */
    public static Map<Class<?>, KeyManager<?>> newMap(final Object[][] config) {
        final Map<Class<?>, KeyManager<?>> managers = new HashMap<>(
                initialCapacity(config.length));
        for (final Object[] param : config) {
            final Class<?> type = Objects.requireNonNull(
                    ServiceLocator.promote(param[0], Class.class));
            final KeyManager<?> manager = ServiceLocator.promote(param[1], KeyManager.class);
            managers.put(type, manager);
        }
        return Collections.unmodifiableMap(managers);
    }

    @Override
    @SuppressWarnings("unchecked")
    public final @CheckForNull <K> KeyManager<K> keyManager(final Class<K> type) {
        return (KeyManager<K>) getKeyManagers().get(type);
    }

    /**
     * Returns an immutable map of secret key classes to nullable key managers.
     * Only the values of the returned map may be {@code null}.
     * <p>
     * This is an immutable property - multiple calls must return the same
     * object.
     * 
     * @return An immutable map of secret key classes to nullable key managers.
     */
    public abstract Map<Class<?>, KeyManager<?>> getKeyManagers();

    /**
     * Returns a string representation of this object for debugging and logging
     * purposes.
     */
    @Override
    public String toString() {
        return String.format("%s[keyManagers=%s]",
                getClass().getName(),
                getKeyManagers());
    }
}
