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
import edu.umd.cs.findbugs.annotations.DefaultAnnotation;
import edu.umd.cs.findbugs.annotations.NonNull;

/**
 * The parameters of this interface are used with WinZip AES encrypted entries.
 *
 * @since   TrueZIP 7.3
 * @see     <a href="http://www.winzip.com/win/en/aes_info.htm">AES Encryption Information: Encryption Specification AE-1 and AE-2</a> (WinZip Computing, S.L.)
 * @see     <a href="http://www.winzip.com/win/en/aes_tips.htm">AES Coding Tips for Developers</a> (WinZip Computing, S.L.)
 * @author  Christian Schlichtherle
 * @version $Id$
 */
@DefaultAnnotation(NonNull.class)
public interface WinZipAesParameters extends ZipCryptoParameters {

    /**
     * Returns the password bytes to use for writing a WinZip AES entry.
     * <p>
     * WARNING: Unfortunately, the document
     * <a href="http://www.winzip.com/win/en/aes_info.htm">AES Encryption Information: Encryption Specification AE-1 and AE-2</a>
     * does not specify how password strings should get encoded to bytes.
     * This means that whatever encoding you chose in the implementation,
     * chances are that an authorized third party will not be able to read
     * WinZip AES entries you have written.
     * This could even be your future self when you are using another
     * application or platform then!
     * <p>
     * There are several reasonable schemes you could chose to encode password
     * characters to bytes:
     * <ul>
     * <li>You could follow the recommendation in the document
     *     <a href="http://www.winzip.com/win/en/aes_tips.htm">AES Coding Tips for Developers</a>
     *     and limit the character set to the non-control characters of
     *     US-ASCII.
     *     However, this severely limits the password search space for brute
     *     force attacks.
     * <li>You could use the same character set which is used to encode entry
     *     names and comments in the ZIP file, e.g. UTF-8 or CP437.
     *     This should be the preferred choice, but would not conform
     *     to WinZip's recommendation.
     *     In the case of encodings other than UTF-8, this would still limit
     *     the password search space, but not as much as US-ASCII.
     * <li>If you are not concerned about interoperability with authorized
     *     third parties, including yourself when using another application or
     *     platform, then you could simply use UTF-8.
     *     This would provide the largest password search space and follow the
     *     convention for JAR files, but again, this would not conform to
     *     WinZip's recommendation.
     * </ul>
     * <p>
     * A reasonable alternative implementation could encode the given char
     * array using the same character set which is used to encode entry names
     * and comments, e.g. UTF-8 or CP437.
     *
     * @param  name the ZIP entry name.
     * @return A clone of the byte array holding the password to use
     *         for writing a WinZip AES entry.
     * @throws ZipKeyException If key retrieval has failed for some reason.
     */
    byte[] getWritePassword(String name)
    throws ZipKeyException;

    /**
     * Returns the password bytes to use for reading a WinZip AES entry.
     * This method is called consecutively until either the returned password
     * bytes are successfully validated or an exception is thrown.
     * <p>
     * WARNING: Unfortunately, the document
     * <a href="http://www.winzip.com/win/en/aes_info.htm">AES Encryption Information: Encryption Specification AE-1 and AE-2</a>
     * does not specify how password strings should get encoded to bytes.
     * This means that whatever encoding you chose in the implementation,
     * chances are that you will not be able to read WinZip AES entries a third
     * party has written.
     * This could even be your past self when you were using another
     * application or platform at the time!
     *
     * @see    #getWritePassword(String)
     * @param  name the ZIP entry name.
     * @param  invalid {@code true} iff a previous call to this method returned
     *         an invalid password.
     * @return A clone of the byte array holding the password to use
     *         for reading a WinZip AES entry.
     * @throws ZipKeyException If key retrieval has failed for some reason.
     */
    byte[] getReadPassword(String name, boolean invalid)
    throws ZipKeyException;

    /**
     * Returns the key strength to use for writing a WinZip AES entry.
     *
     * @param  name the ZIP entry name.
     * @return The key strength to use for writing a WinZip AES entry.
     * @throws ZipKeyException If key retrieval has failed for some reason.
     */
    AesKeyStrength getKeyStrength(String name)
    throws ZipKeyException;

    /**
     * Sets the key strength obtained from reading a WinZip AES entry.
     *
     * @param  name the ZIP entry name.
     * @param  keyStrength the key strength obtained from reading a WinZip AES
     *         entry.
     * @throws ZipKeyException If key retrieval has failed for some reason.
     */
    void setKeyStrength(String name, AesKeyStrength keyStrength)
    throws ZipKeyException;
}
