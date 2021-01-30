/*
 * Copyright (C) 2005-2015 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truecommons.key.spec;

import javax.annotation.concurrent.Immutable;
import net.java.truecommons.shed.UniqueObject;

/**
 * An abstract key provider.
 * When implementing a key provider, you should extend this class rather
 * than directly implementing the interface in order to maintain binary
 * backwards compatibility even if the interface is changed.
 * <p>
 * Implementations must be safe for multi-threading.
 *
 * @param  <K> The type of the keys.
 * @since TrueCommons 2.2
 * @author Christian Schlichtherle
 */
@Immutable
public abstract class AbstractKeyProvider<K>
extends UniqueObject implements KeyProvider<K> {

    /**
     * Returns a string representation of this object for logging and debugging
     * purposes.
     */
    @Override
    public String toString() {
        return String.format("%s", getClass().getName());
    }
}
