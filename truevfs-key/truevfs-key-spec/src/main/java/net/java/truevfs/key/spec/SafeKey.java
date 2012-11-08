/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.key.spec;

/**
 * A safe key for writing and reading protected resources.
 * <p>
 * Implementations do <em>not</em> need to be safe for multi-threading.
 *
 * @param  <K> the type of this safe key.
 * @author Christian Schlichtherle
 */
public interface SafeKey<K extends SafeKey<K>> extends Cloneable {

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
}
