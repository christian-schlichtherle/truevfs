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
package de.schlichtherle.truezip.crypto.raes.param;

import de.schlichtherle.truezip.crypto.raes.Type0RaesParameters.KeyStrength;
import de.schlichtherle.truezip.key.SafeKey;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.DefaultAnnotation;
import java.util.Arrays;
import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;

import static de.schlichtherle.truezip.crypto.raes.Type0RaesParameters.KeyStrength.*;

/**
 * A Java bean which holds AES cipher parameters.
 *
 * @author  Christian Schlichtherle
 * @version $Id$
 */
@DefaultAnnotation(NonNull.class)
public final class AesCipherParameters
implements SafeKey<AesCipherParameters>, Cloneable {

    private KeyStrength keyStrength = BITS_256;
    private @CheckForNull char[] password;

    @Override
    public AesCipherParameters clone() {
        try {
            AesCipherParameters clone = (AesCipherParameters) super.clone();
            if (null != password)
                clone.password = password.clone();
            return clone;
        } catch (CloneNotSupportedException ex) {
            throw new AssertionError(ex);
        }
    }

    @Override
    public void reset() {
        setPassword(null);
        setKeyStrength(BITS_256);
    }

    /**
     * Returns a protective copy of the password char array.
     *
     * @return A protective copy of the password char array.
     */
    public @Nullable char[] getPassword() {
        return null == password ? null : password.clone();
    }

    /**
     * Copies the given password char array for subsequent use as the AES
     * cipher key.
     * <p>
     * This method makes a protective copy of the given password char array.
     * It's highly recommended to overwrite this array with any non-password
     * data after calling this method.
     *
     * @param newPW the password char array to be copied for subsequent use.
     */
    public void setPassword(final @CheckForNull char[] newPW) {
        final char[] oldPW = this.password;
        if (null != oldPW)
            Arrays.fill(oldPW, (char) 0);
        this.password = null == newPW ? null : newPW.clone();
    }

    /**
     * Decodes the given byte array to a password char array for subsequent use.
     * This method should be used if a key file is used rather than a password.
     * <p>
     * This method makes a protective copy of the given key file byte array.
     * It's highly recommended to overwrite this array with any non-password
     * data after calling this method.
     *
     * @param bytes the byte array to decode.
     */
    public void setKeyFileBytes(final @CheckForNull byte[] bytes) {
        // Do NOT use the following - it would omit a byte order sequence
        // and cannot decode all characters.
        // return new String(buf, 0, n, "UTF-16BE").toCharArray();

        // Decode the characters from UTF-16BE, so that the byte order
        // is preserved when the char array is later again translated
        // to a byte array again according to PKCS #12, section B.1.
        final char[] oldPW = this.password;
        if (null != oldPW)
            Arrays.fill(oldPW, (char) 0);
        if (null != bytes) {
            int len = bytes.length;
            len >>= 1;
            final char[] newPW = new char[len];
            for (int i = 0, off = 0; i < len; i++)
                newPW[i] = (char) (bytes[off++] << 8 | bytes[off++] & 0xFF); // attention!
            this.password = newPW;
        } else {
            this.password = null;
        }
    }

    /**
     * Returns the AES cipher key strength, which defaults to
     * {@link KeyStrength#BITS_256}.
     *
     * @return The AES cipher key strength, which defaults to
     *         {@link KeyStrength#BITS_256}.
     */
    public KeyStrength getKeyStrength() {
        return keyStrength;
    }

    /**
     * Sets the AES cipher key strength.
     *
     * @param keyStrength the AES cipher key strength.
     */
    public void setKeyStrength(final KeyStrength keyStrength) {
        if (null == keyStrength)
            throw new NullPointerException();
        this.keyStrength = keyStrength;
    }
}
