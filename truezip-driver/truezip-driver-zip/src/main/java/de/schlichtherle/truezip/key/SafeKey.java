/*
 * Copyright 2004-2012 Schlichtherle IT Services
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package de.schlichtherle.truezip.key;

import javax.annotation.concurrent.NotThreadSafe;

/**
 * A safe secret key for the encryption and decryption of protected resources.
 * <p>
 * Implementations of this interface do not need to be thread-safe.
 *
 * @author  Christian Schlichtherle
 * @version $Id$
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
