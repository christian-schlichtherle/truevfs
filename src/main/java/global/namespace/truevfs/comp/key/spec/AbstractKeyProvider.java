/*
 * Copyright (C) 2005-2015 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package global.namespace.truevfs.comp.key.spec;

import global.namespace.truevfs.comp.shed.UniqueObject;

/**
 * An abstract key provider.
 * When implementing a key provider, you should extend this class rather
 * than directly implementing the interface in order to maintain binary
 * backwards compatibility even if the interface is changed.
 * <p>
 * Implementations must be safe for multi-threading.
 *
 * @param  <K> The type of the keys.
 * @author Christian Schlichtherle
 */
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
