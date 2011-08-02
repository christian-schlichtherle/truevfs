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

import de.schlichtherle.truezip.zip.aes.AesKeyStrength;
import edu.umd.cs.findbugs.annotations.DefaultAnnotation;
import edu.umd.cs.findbugs.annotations.NonNull;

/**
 * The parameters of this interface are used with WinZip AES encrypted entries.
 *
 * @see     <a href="http://www.winzip.com/win/en/aes_info.htm">AES Encryption Information: Encryption Specification AE-1 and AE-2</a>
 * @author  Christian Schlichtherle
 * @version $Id$
 */
@DefaultAnnotation(NonNull.class)
public interface WinZipAesParameters extends ZipCryptoParameters {

    /**
     * Returns the password to use for writing a WinZip AES entry.
     *
     * @param  name the ZIP entry name.
     * @return A clone of the char array holding the password to use
     *         for writing a WinZip AES entry.
     * @throws ZipKeyException If password retrieval has failed for some
     *         reason.
     */
    char[] getWritePassword(String name) throws ZipKeyException;

    /**
     * Returns the password to use for reading a WinZip AES entry.
     * This method is called consecutively until either the returned password
     * is successfully validated or an exception is thrown.
     *
     * @param  name the ZIP entry name.
     * @param  invalid {@code true} iff a previous call to this method resulted
     *         in an invalid password.
     * @return A clone of the char array holding the password to use
     *         for reading a WinZip AES entry.
     * @throws ZipKeyException If password retrieval has failed for some
     *         reason.
     */
    char[] getReadPassword(String name, boolean invalid) throws ZipKeyException;

    /**
     * Returns the key strength to use for writing a WinZip AES entry.
     *
     * @param  name the ZIP entry name.
     * @return The key strength to use for writing a WinZip AES entry.
     * @throws RuntimeException if {@link #getWritePassword(String)}
     *         hasn't been called before and the implementation can't tolerate
     *         this.
     */
    AesKeyStrength getKeyStrength(String name);

    /**
     * Sets the key strength obtained from reading a WinZip AES entry.
     *
     * @param  name the ZIP entry name.
     * @param  keyStrength the key strength obtained from reading a WinZip AES
     *         entry.
     * @throws RuntimeException if {@link #getReadPassword(String, boolean)}
     *         hasn't been called before and the implementation can't tolerate
     *         this.
     */
    void setKeyStrength(String name, AesKeyStrength keyStrength);
}
