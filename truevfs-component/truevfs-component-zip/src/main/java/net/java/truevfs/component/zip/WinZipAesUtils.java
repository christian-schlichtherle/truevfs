/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.component.zip;

import net.java.truevfs.keymanager.spec.param.AesKeyStrength;

/**
 * Static utility methods for WinZip AES encryption.
 * 
 * @author  Christian Schlichtherle
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