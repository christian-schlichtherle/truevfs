/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.key.spec;

import javax.annotation.concurrent.ThreadSafe;
import net.java.truecommons.shed.UniqueObject;

/**
 * A container for key providers for reading and writing protected resources.
 * <p>
 * Implementations must be thread-safe.
 *
 * @param  <K> The type of the secret keys.
 * @author Christian Schlichtherle
 */
@ThreadSafe
public abstract class AbstractKeyManager<K>
extends UniqueObject implements KeyManager<K> {

    /**
     * Returns a string representation of this object for debugging and logging
     * purposes.
     */
    @Override
    public String toString() {
        return String.format("%s", getClass().getName());
    }
}
