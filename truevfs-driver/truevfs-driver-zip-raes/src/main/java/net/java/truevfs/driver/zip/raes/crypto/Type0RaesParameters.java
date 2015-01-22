/*
 * Copyright (C) 2005-2015 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.driver.zip.raes.crypto;

import net.java.truecommons.key.spec.common.AesKeyStrength;

/**
 * The parameters of this interface are used with RAES <i>type 0</i> files.
 * Type 0 RAES files use password based encryption according to the
 * specifications in PKCS #5 V2.0 und PKCS #12 V1.0.
 * <p>
 * Implementations do not need to be safe for multi-threading.
 *
 * @see     <a href="http://www.rsasecurity.com/rsalabs/pkcs/pkcs-5/index.html">PKCS #5</a>
 * @see     <a href="http://www.rsasecurity.com/rsalabs/pkcs/pkcs-12/index.html">PKCS #12</a>
 * @author  Christian Schlichtherle
 */
public interface Type0RaesParameters extends RaesParameters {

    /**
     * Returns the password to use for writing a RAES type 0 file.
     *
     * @return A clone of the char array holding the password to use
     *         for writing a RAES type 0 file.
     * @throws RaesKeyException If key retrieval has failed for some reason.
     */
    char[] getPasswordForWriting() throws RaesKeyException;

    /**
     * Returns the password to use for reading a RAES type 0 file.
     * This method is called consecutively until either the returned password
     * is successfully validated or an exception is thrown.
     *
     * @param  invalid {@code true} iff a previous call to this method returned
     *         an invalid password.
     * @return A clone of the char array holding the password to use
     *         for reading a RAES type 0 file.
     * @throws RaesKeyException If key retrieval has failed for some reason.
     */
    char[] getPasswordForReading(boolean invalid) throws RaesKeyException;

    /**
     * Returns the key strength to use for writing a RAES type 0 file.
     *
     * @return The key strength to use for writing a RAES type 0 file.
     * @throws RaesKeyException If key retrieval has failed for some reason.
     */
    AesKeyStrength getKeyStrength() throws RaesKeyException;

    /**
     * Sets the key strength obtained from reading a RAES type 0 file.
     *
     * @param  keyStrength the key strength obtained from reading a RAES type 0
     *         file.
     * @throws RaesKeyException If key retrieval has failed for some reason.
     */
    void setKeyStrength(AesKeyStrength keyStrength) throws RaesKeyException;
}