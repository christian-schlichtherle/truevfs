/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.keymanager.spec;

import net.java.truecommons.services.Container;
import java.util.Map;
import javax.annotation.CheckForNull;
import javax.annotation.concurrent.Immutable;

/**
 * An abstract provider for an immutable map of secret key classes to nullable
 * key managers.
 *
 * @author Christian Schlichtherle
 */
@Immutable
public abstract class AbstractKeyManagerContainer
implements KeyManagerContainer, Container<Map<Class<?>, KeyManager<?>>> {

    @Override
    @SuppressWarnings("unchecked")
    public final @CheckForNull <K> KeyManager<K> keyManager(final Class<K> type) {
        return (KeyManager<K>) get().get(type);
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
    @Override
    public abstract Map<Class<?>, KeyManager<?>> get();

    /**
     * Returns a string representation of this object for debugging and logging
     * purposes.
     */
    @Override
    public String toString() {
        return String.format("%s[keyManagers=%s]",
                getClass().getName(),
                get());
    }
}
