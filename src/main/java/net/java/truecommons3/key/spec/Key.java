/*
 * Copyright (C) 2005-2015 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truecommons3.key.spec;

import net.java.truecommons3.shed.ImplementationsShouldExtend;

import javax.annotation.Nullable;

/**
 * A generic key with basic features for life cycle management.
 * <p>
 * Implementations do not need to be safe for multi-threading.
 *
 * @param  <K> the type of this key.
 * @since TrueCommons 2.2
 * @author Christian Schlichtherle
 */
@ImplementationsShouldExtend(AbstractKey.class)
public interface Key<K extends Key<K>> extends Cloneable {

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
    @Override boolean equals(final @Nullable Object obj);

    /**
     * Returns a hash code which is consistent with {@link #equals(Object)}.
     * This method is provided for completeness only - you should actually
     * never use secret keys as hash map keys because of their mutable
     * properties!
     */
    @Override int hashCode();
}
