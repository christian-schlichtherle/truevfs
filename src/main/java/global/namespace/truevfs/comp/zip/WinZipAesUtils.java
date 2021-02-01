/*
 * Copyright © 2005 - 2021 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package global.namespace.truevfs.comp.zip;

import global.namespace.truevfs.comp.key.api.common.AesKeyStrength;

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