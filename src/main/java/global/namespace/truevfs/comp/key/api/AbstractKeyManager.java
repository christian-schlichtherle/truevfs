/*
 * Copyright (C) 2005-2015 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package global.namespace.truevfs.comp.key.api;

/**
 * An abstract key manager.
 * When implementing a key manager, you should extend this class rather
 * than directly implementing the interface in order to maintain binary
 * backwards compatibility even if the interface is changed.
 * <p>
 * Subclasses must be safe for multi-threading.
 *
 * @param  <K> The type of the keys.
 * @author Christian Schlichtherle
 */
public abstract class AbstractKeyManager<K> implements KeyManager<K> {

    /**
     * Returns a string representation of this object for logging and debugging
     * purposes.
     */
    @Override
    public String toString() {
        return String.format("%s", getClass().getName());
    }
}
