/*
 * Copyright 2004-2012 Schlichtherle IT Services
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package de.schlichtherle.truezip.crypto.param;

import net.jcip.annotations.ThreadSafe;

/**
 * Defines the key strengths for a cipher.
 * <p>
 * Implementations must be thread-safe!
 *
 * @author  Christian Schlichtherle
 * @version $Id$
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
