/*
 * Copyright (C) 2005-2015 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package global.namespace.truevfs.comp.key.api;

import javax.annotation.Nullable;
import java.io.Serializable;

/**
 * A generic key with basic features for life cycle management.
 * Implementations need to be serializable with {@code Object(Out|In)putStream} and {@code XML(En|De)coder}.
 * Due to it's mutable state, implementations do not need to be thread-safe.
 *
 * @param <K> the type of this key.
 * @author Christian Schlichtherle
 */
public interface Key<K extends Key<K>> extends Cloneable, Serializable {

    /**
     * Wipes the secret data from memory and resets all properties to their initial state.
     */
    void reset();

    /**
     * Returns a deep clone of this key.
     * The returned object does not share any mutable state with this object.
     */
    K clone();

    /**
     * Returns {@code true} if and only if this key deeply equals the given object.
     * A key equals another object if and only if the other object has the same runtime class and all its properties
     * compare deeply equal.
     */
    @Override
    boolean equals(@Nullable Object obj);

    /**
     * Returns a hash code which is consistent with {@link #equals(Object)}.
     * This method is provided for completeness only - you should actually never use a key as a hash map key because of
     * its mutable state!
     */
    @Override
    int hashCode();
}
