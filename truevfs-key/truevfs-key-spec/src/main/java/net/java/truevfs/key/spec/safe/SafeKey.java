/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.key.spec.safe;

/**
 * A safe key for writing and reading protected resources.
 * <p>
 * Implementations do not need to be safe for multi-threading.
 *
 * @param  <K> the type of this safe key.
 * @author Christian Schlichtherle
 */
public interface SafeKey<K extends SafeKey<K>> extends Cloneable {

    /**
     * Wipes the secret data from memory and resets all properties to their
     * initial state.
     */
    void reset();

    /**
     * Returns a deep clone of this safe key.
     * The returned object does not share any mutable state with this object.
     */
    K clone();

    /**
     * Returns {@code true} if and only if this safe key deeply equals the
     * given object.
     * A safe key equals another object if and only if the other object
     * has the same runtime class and all its properties compare deeply equal.
     */
    @Override boolean equals(final Object obj);

    /**
     * Returns a hash code which is consistent with {@link #equals(Object)}.
     * This method is provided for completeness only - you should actually
     * never use secret keys as hash map keys because of their mutable
     * properties!
     */
    @Override int hashCode();
}
