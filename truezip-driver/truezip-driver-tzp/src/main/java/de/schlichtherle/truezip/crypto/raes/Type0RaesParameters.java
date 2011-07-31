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
import net.jcip.annotations.NotThreadSafe;
import net.jcip.annotations.ThreadSafe;

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
@NotThreadSafe
@DefaultAnnotation(NonNull.class)
public interface Type0RaesParameters extends RaesParameters {

    /**
     * Returns the password required to create or overwrite the RAES type 0 file.
     *
     * @return A clone of the char array holding the password to use for
     *         creating or overwriting the RAES file.
     * @throws RaesKeyException If password retrieval has been disabled or
     *         cancelled.
     */
    char[] getWritePassword() throws RaesKeyException;

    /**
     * Returns the password required to open the RAES type 0 file for reading.
     * This method is called consecutively until either the returned password
     * is correct or an exception is thrown.
     *
     * @param  invalid {@code true} iff a previous call to this method resulted
     *         in an invalid password.
     * @return A clone of the char array holding the password to open the RAES
     *         file for reading.
     * @throws RaesKeyException If password retrieval has been disabled or
     *         cancelled.
     */
    char[] getReadPassword(boolean invalid) throws RaesKeyException;

    /**
     * Returns the key strength to use for creating or overwriting the RAES file.
     *
     * @return The key strength to use for creating or overwriting the RAES file.
     * @throws RuntimeException if {@link #getWritePassword()} hasn't
     *         been called before and the implementation can't tolerate this.
     */
    KeyStrength getKeyStrength();

    /**
     * Sets the key strength to use for creating or overwriting the RAES file.
     *
     * @param keyStrength the key strength to use for creating or overwriting
     *        the RAES file.
     * @throws RuntimeException if {@link #getReadPassword(boolean)} hasn't
     *         been called before and the implementation can't tolerate this.
     */
    void setKeyStrength(KeyStrength keyStrength);

    /** Enumerates the AES cipher key strenghts. */
    @ThreadSafe
    enum KeyStrength implements de.schlichtherle.truezip.key.pbe.KeyStrength {
        /** Enum identifier for a 128 bit AES cipher key. */
        BITS_128,

        /** Enum identifier for a 192 bit AES cipher key. */
        BITS_192,

        /** Enum identifier for a 256 bit AES cipher key. */
        BITS_256;

        private static final ResourceBundle resources
                = ResourceBundle.getBundle(KeyStrength.class.getName());

        /** Returns the key strength in bytes. */
        @Override
        public int getBytes() {
            return 16 + 8 * ordinal();
        }

        /** Returns the key strength in bits. */
        @Override
        public int getBits() {
            return 8 * getBytes();
        }

        @Override
        public String toString() {
            return resources.getString(name());
        }
    }
}
