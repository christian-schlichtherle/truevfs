/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.key.spec.safe;

import javax.annotation.CheckForNull;

/**
 * A safe key for writing and reading protected resources.
 * <p>
 * Implementations do not need to be safe for multi-threading.
 *
 * @param  <K> the type of this safe key.
 * @author Christian Schlichtherle
 */
public interface SafeKey<
        K extends SafeKey<K, S>,
        S extends SafeKeyStrength>
extends Cloneable {

    /**
     * Returns a deep clone of this safe key.
     *
     * @return A deep clone of this safe key.
     */
    K clone();

    /**
     * Clears any key data from memory and resets this safe key to it's initial
     * state.
     */
    void reset();

    /**
     * Returns the cipher key strength.
     *
     * @return The cipher key strength.
     */
    @CheckForNull S getKeyStrength();

    /**
     * Sets the cipher key strength.
     *
     * @param keyStrength the cipher key strength.
     */
    void setKeyStrength(@CheckForNull S keyStrength);

    /**
     * A safe key equals another object if and only if the other object
     * has the same runtime class and its properties compare equal.
     * In other words, this is a deep-equals comparison.
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
