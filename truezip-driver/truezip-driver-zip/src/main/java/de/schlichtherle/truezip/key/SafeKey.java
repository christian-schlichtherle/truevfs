/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.key;

import javax.annotation.concurrent.NotThreadSafe;

/**
 * A safe secret key for the encryption and decryption of protected resources.
 * <p>
 * Implementations of this interface do not need to be thread-safe.
 *
 * @author  Christian Schlichtherle
 */
@NotThreadSafe
public interface SafeKey<K> extends Cloneable {

    /** Returns a deep clone of this safe key. */
    K clone();

    /**
     * Wipes any key data from the heap and resets this safe key to it's
     * initial state.
     */
    void reset();
}