/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.truezip.key.param;

import de.truezip.key.SafeKey;
import java.util.Arrays;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;

/**
 * A JavaBean which holds parameters for password based encryption.
 * <p>
 * Sub classes do not need to be thread-safe.
 *
 * @param  <P> the type of these safe PBE parameters.
 * @param  <S> the type of the key strength.
 * @author Christian Schlichtherle
 */
@NotThreadSafe
public abstract class SafePbeParameters<
        P extends SafePbeParameters<P, S>,
        S extends KeyStrength>
implements SafeKey<P> {

    private @CheckForNull char[] password;
    private @CheckForNull S keyStrength;

    @Override
    @SuppressWarnings("unchecked")
    public P clone() {
        final SafePbeParameters<P, S> clone;
        try {
             clone = (SafePbeParameters<P, S>) super.clone();
        } catch (CloneNotSupportedException ex) {
            throw new AssertionError(ex);
        }
        final char[] password = this.password;
        if (null != password)
            clone.password = password.clone();
        return (P) clone;
    }

    @Override
    public void reset() {
        setPassword(null);
        setKeyStrength(null);
    }

    /**
     * Returns a protective copy of the stored password char array.
     *
     * @return A protective copy of the stored password char array.
     */
    public @Nullable char[] getPassword() {
        return null == password ? null : password.clone();
    }

    /**
     * Copies and stores the given password char array for deriving the cipher
     * key.
     * It's highly recommended to overwrite this array with any non-password
     * data after calling this method.
     *
     * @param newPW the password char array for deriving the cipher key.
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
     * Returns a new non-empty array of all available key strength values.
     * There should be no duplicated elements in this array.
     *
     * @return A new non-empty array of all available key strength values.
     */
    public abstract S[] getKeyStrengthValues();

    /**
     * Returns the cipher key strength.
     *
     * @return The cipher key strength.
     */
    public @Nullable S getKeyStrength() {
        return keyStrength;
    }

    /**
     * Sets the cipher key strength.
     *
     * @param keyStrength the cipher key strength.
     */
    public void setKeyStrength(final @CheckForNull S keyStrength) {
        this.keyStrength = keyStrength;
    }
}
