/*
 * Copyright (C) 2011 Schlichtherle IT Services
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package de.schlichtherle.truezip.zip;

import de.schlichtherle.truezip.crypto.param.AesKeyStrength;

/**
 * Static utility methods for WinZip AES encryption.
 * 
 * @author  Christian Schlichtherle
 * @version $Id$
 */
final class WinZipAesUtils {

    /** You cannot instantiate this class. */
    private WinZipAesUtils() {
    }

    /** 
     * Returns the overhead in bytes which is added to each WinZip AES
     * encrypted entry.
     * 
     * @param  keyStrength The AES key strength.
     * @return the overhead in bytes which is added to each WinZip AES
     *         encrypted entry.
     */
    static int overhead(AesKeyStrength keyStrength) {
        return keyStrength.getBytes() / 2 // salt value
                + 2   // password verification value
                + 10; // authentication code
    }
}
