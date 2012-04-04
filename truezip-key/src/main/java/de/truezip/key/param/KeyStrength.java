/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.truezip.key.param;

import javax.annotation.concurrent.ThreadSafe;

/**
 * Defines the key strengths for a cipher.
 * <p>
 * Implementations must be thread-safe!
 *
 * @author  Christian Schlichtherle
 */
@ThreadSafe
public interface KeyStrength {

    /**
     * Returns the index of the key strength.
     * 
     * @return The index of the key strength.
     */
    int ordinal();

    /**
     * Returns the key strength in bits.
     * 
     * @return The key strength in bits.
     */
    int getBits();

    /**
     * Returns the key strength in bytes.
     * 
     * @return The key strength in bytes.
     */
    int getBytes();

    /**
     * Returns a localized display string representing this key strength.
     * 
     * @return A localized display string representing this key strength.
     */
    @Override
    String toString();
}