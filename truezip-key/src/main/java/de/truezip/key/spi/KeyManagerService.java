/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.truezip.key.spi;

import de.truezip.key.AbstractKeyManagerProvider;
import de.truezip.key.sl.KeyManagerLocator;
import javax.annotation.concurrent.Immutable;

/**
 * An abstract locatable service for key managers.
 * Implementations of this abstract class are subject to service location
 * by the class {@link KeyManagerLocator}.
 * <p>
 * Implementations must be thread-safe.
 *
 * @author Christian Schlichtherle
 */
@Immutable
public abstract class KeyManagerService extends AbstractKeyManagerProvider {

    /**
     * Returns a priority to help the key manager service locator.
     * The greater number wins!
     * The default value should be zero.
     * 
     * @return A priority to help the key manager service locator.
     */
    public abstract int getPriority();

    /**
     * Returns a string representation of this object for debugging and logging
     * purposes.
     */
    @Override
    public String toString() {
        return String.format("%s[priority=%d, keyManagers=%s]",
                getClass().getName(),
                getPriority(),
                getKeyManagers());
    }
}
