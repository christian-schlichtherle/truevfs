/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.truevfs.keymgr.spec;

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
public abstract class AbstractKeyManagerProvider
implements KeyManagerProvider {

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
