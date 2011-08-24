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

import edu.umd.cs.findbugs.annotations.DefaultAnnotation;
import edu.umd.cs.findbugs.annotations.NonNull;
import java.util.ResourceBundle;
import net.jcip.annotations.ThreadSafe;

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
 * @version $Id$
 */
@DefaultAnnotation(NonNull.class)
public interface Type0RaesParameters extends RaesParameters {

    /**
     * Returns the password to use for writing a RAES type 0 file.
     *
     * @return A clone of the char array holding the password to use
     *         for writing a RAES type 0 file.
     * @throws RaesKeyException If key retrieval has failed for some reason.
     */
    char[] getWritePassword() throws RaesKeyException;

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
    char[] getReadPassword(boolean invalid) throws RaesKeyException;

    /**
     * Returns the key strength to use for writing a RAES type 0 file.
     *
     * @return The key strength to use for writing a RAES type 0 file.
     * @throws RaesKeyException If key retrieval has failed for some reason.
     */
    KeyStrength getKeyStrength() throws RaesKeyException;

    /**
     * Sets the key strength obtained from reading a RAES type 0 file.
     *
     * @param  keyStrength the key strength obtained from reading a RAES type 0
     *         file.
     * @throws RaesKeyException If key retrieval has failed for some reason.
     */
    void setKeyStrength(KeyStrength keyStrength) throws RaesKeyException;

    /** Enumerates the AES cipher key strenghts. */
    @ThreadSafe
    enum KeyStrength implements de.schlichtherle.truezip.crypto.param.KeyStrength {
        /** Enum identifier for a 128 bit AES cipher key. */
        BITS_128,

        /** Enum identifier for a 192 bit AES cipher key. */
        BITS_192,

        /** Enum identifier for a 256 bit AES cipher key. */
        BITS_256;

        private static final ResourceBundle resources
                = ResourceBundle.getBundle(KeyStrength.class.getName());

        @Override
        public int getBytes() {
            return 16 + 8 * ordinal();
        }

        @Override
        public int getBits() {
            return 8 * getBytes();
        }

        @Override
        public String toString() {
            return resources.getString(name());
        }
    } // KeyStrength
}
