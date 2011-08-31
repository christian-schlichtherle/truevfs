/*
 * Copyright (C) 2005-2011 Schlichtherle IT Services
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
package de.schlichtherle.truezip.crypto.raes;

/**
 * This interfaces contains constants used to read or write files
 * according to the Random Access Encryption Specification (Constants).
 * Public classes <em>must not</em> implement this interface - otherwise the
 * constants become part of the public API.
 *
 * @author  Christian Schlichtherle
 * @version $Id$
 */
interface Constants {

    /**
     * The signature of any Constants compatible content in little endian format
     * ({@code "Constants"} as a US-ASCII character sequence).
     */
    int SIGNATURE = 'R' | (('A' | (('E' | ('S' << 8)) << 8)) << 8);

    int LEAD_IN_LENGTH =
            4 + // SIGNATURE
            1;  // Envelope type

    /**
     * The data envelope type used for password based encryption
     * with the same salt length as the cipher key length.
     */
    byte ENVELOPE_TYPE_0 = 0;

    /** The length of the header before the salt and the encrypted data. */
    int ENVELOPE_TYPE_0_HEADER_LEN_WO_SALT =
            LEAD_IN_LENGTH +
            1 + // Cipher and MAC key strength.
            2;  // Iteration count
                // The salt which's length is the cipher key length.
                // The ciphered data which has the same length as the plain data.
                // The KLAC (first half of 256 bit SHA output = 128 bits).
                // The  MAC (first half of 256 bit SHA output = 128 bits).

    /**
     * The data envelope type reserved for certificate based encryption and
     * authentication.
     * This type is not yet specified, but reserved for future use.
     */
    byte ENVELOPE_TYPE_1 = 1;

    /**
     * The block size of the Advanced Encryption Specification (AES) Algorithm
     * in bits ({@value #AES_BLOCK_SIZE_BITS}).
     */
    int AES_BLOCK_SIZE_BITS = 128;
}
