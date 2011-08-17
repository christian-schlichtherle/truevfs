/*
 * Copyright (C) 2011 Schlichtherle IT Services
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
