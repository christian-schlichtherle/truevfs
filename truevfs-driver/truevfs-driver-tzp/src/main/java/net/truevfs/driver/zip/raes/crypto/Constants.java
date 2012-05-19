/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.truevfs.driver.zip.raes.crypto;

/**
 * This interfaces contains constants used to read or write files
 * according to the Random Access Encryption Specification (Constants).
 * Public classes <em>must not</em> implement this interface - otherwise the
 * constants become part of the public API.
 *
 * @author  Christian Schlichtherle
 */
interface Constants {

    /**
     * The signature of any Constants compatible content in little endian format
     * ({@code "Constants"} as a US-ASCII character sequence).
     */
    int SIGNATURE = 'R' | (('A' | (('E' | ('S' << 8)) << 8)) << 8);

    int HEADER_MIN_LEN =
            4 + // SIGNATURE
            1;  // Envelope type

    /**
     * The data envelope type used for password based encryption
     * with the same salt length as the cipher key length.
     */
    byte TYPE_0 = 0;

    /** The length of the header before the salt and the encrypted data. */
    int TYPE_0_HEADER_LEN_WO_SALT =
            HEADER_MIN_LEN +
            1 + // Cipher and MAC key strength.
            2;  // Iteration count
                // The salt which's length is the cipher key length.
                // The ciphered data which has the same length as the plain data.
                // The KLAC (first half of 256 bit SHA output = 128 bits).
                // The  MAC (first half of 256 bit SHA output = 128 bits).

    /**
     * The block size of the Advanced Encryption Specification (AES) Algorithm
     * in bits ({@value #AES_BLOCK_SIZE_BITS}).
     */
    int AES_BLOCK_SIZE_BITS = 128;
}