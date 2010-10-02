/*
 * Copyright (C) 2005-2010 Schlichtherle IT Services
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

package de.schlichtherle.truezip.crypto.io.raes;

/**
 * The parameters of this interface are used with RAES <i>type 0</i> files.
 * Type 0 RAES files use password based encryption according to the
 * specifications in PKCS #5 V2.0 und PKCS #12 V1.0.
 *
 * @see <a href="http://www.rsasecurity.com/rsalabs/pkcs/pkcs-5/index.html" target="_blank">PKCS #5</a>
 * @see <a href="http://www.rsasecurity.com/rsalabs/pkcs/pkcs-12/index.html" target="_blank">PKCS #12</a>
 * @author Christian Schlichtherle
 * @version $Id$
 */
public interface Type0RaesParameters extends RaesParameters {

    /**
     * Returns the password required to create or overwrite the RAES type 0 file.
     *
     * @return A clone of the char array holding the password to use for
     *         creating or overwriting the RAES file - never {@code null}.
     * @throws RaesKeyException If password retrieval has been disabled or
     *         cancelled.
     */
    char[] getCreatePasswd() throws RaesKeyException;


    /**
     * Returns the password required to open the RAES type 0 file for reading.
     * Please note that this method is called repeatedly until either the
     * returned password is correct or an exception has been thrown.
     *
     * @return A clone of the char array holding the password to open the RAES
     *         file for reading - never {@code null}.
     * @throws RaesKeyException If password retrieval has been disabled or
     *         cancelled.
     */
    char[] getOpenPasswd() throws RaesKeyException;

    /**
     * Callback to report that the password returned by {@link #getOpenPasswd()}
     * is wrong.
     */
    void invalidOpenPasswd();

    /** Identifier for a 128 bit ciphering key. */
    int KEY_STRENGTH_128 = 0;

    /** Identifier for a 192 bit ciphering key. */
    int KEY_STRENGTH_192 = 1;

    /** Identifier for a 256 bit ciphering key. */
    int KEY_STRENGTH_256 = 2;

    /**
     * Returns the key strength to use for creating or overwriting the RAES file.
     *
     * @see #KEY_STRENGTH_128
     * @see #KEY_STRENGTH_192
     * @see #KEY_STRENGTH_256
     */
    int getKeyStrength();

    /**
     * Sets the key strength which was used when creating or overwriting the
     * RAES file.
     */
    void setKeyStrength(int keyStrength);
}
