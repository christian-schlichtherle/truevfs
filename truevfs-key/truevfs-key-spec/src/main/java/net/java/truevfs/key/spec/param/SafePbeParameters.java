/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.key.spec.param;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import net.java.truevfs.key.spec.AbstractSecretKey;
import static net.java.truevfs.key.spec.BufferUtils.*;
import net.java.truevfs.key.spec.PromptingKey;

/**
 * A JavaBean which holds parameters for password based encryption.
 * Passwords get encoded using {@link StandardCharsets#UTF_8}.
 * <p>
 * Subclasses do <em>not</em> need to be safe for multi-threading.
 *
 * @param  <P> the type of these safe PBE parameters.
 * @param  <S> the type of the key strength.
 * @author Christian Schlichtherle
 */
@NotThreadSafe
public abstract class SafePbeParameters<
        P extends SafePbeParameters<P, S>,
        S extends KeyStrength>
extends AbstractSecretKey<P> implements PromptingKey<P> {

    private @CheckForNull S keyStrength;
    private boolean changeRequested;

    @Override
    public void reset() {
        super.reset();
        keyStrength = null;
        changeRequested = false;
    }

    /**
     * Returns a protective copy of the password char array.
     * It's highly recommended to overwrite the char array with any
     * non-password data after using the password.
     *
     * @return A protective copy of the password char array.
     */
    public @Nullable char[] getPassword() { return charArray(getSecret()); }

    /**
     * Copies and stores the given password char array for deriving the cipher
     * key.
     * It's highly recommended to overwrite the char array with any
     * non-password data after calling this method.
     *
     * @param password the password char array for deriving the cipher key.
     */
    public void setPassword(final @CheckForNull char[] password) {
        setSecret(byteBuffer(password));
    }

    /**
     * Decodes the given byte array to a password char array for subsequent use.
     * This method should be used if a key file is used rather than a password.
     * <p>
     * This method makes a protective copy of the given key file byte array.
     * It's highly recommended to overwrite the byte array with any
     * non-password data after calling this method.
     *
     * @param bytes the byte array to decode.
     */
    public void setKeyFileBytes(final @CheckForNull byte[] bytes) {
        // Recode the bytes as UTF-16BE encoded characters.
        // This preserves the byte order when the password char array is later
        // encoded to a byte array again in accordance with PKCS #12,
        // section B.1.
        final ByteBuffer bb = byteBuffer(ByteBuffer.wrap(bytes).asCharBuffer());
        try {
            setSecret(bb);
        } finally {
            fill(bb, (byte) 0);
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
    public @CheckForNull S getKeyStrength() { return keyStrength; }

    /**
     * Sets the cipher key strength.
     *
     * @param keyStrength the cipher key strength.
     */
    public void setKeyStrength(final @CheckForNull S keyStrength) {
        this.keyStrength = keyStrength;
    }

    @Override
    public boolean isChangeRequested() { return changeRequested; }

    @Override
    public void setChangeRequested(final boolean changeRequested) {
        this.changeRequested = changeRequested;
    }

    /**
     * Safe PBE parameters equal another object if and only if the other object
     * has the same runtime class and their properties compare equal.
     */
    @Override
    @SuppressWarnings("AccessingNonPublicFieldOfAnotherObject")
    public final boolean equals(final Object obj) {
        if (this == obj) return true;
        if (!super.equals(obj)) return false;
        final SafePbeParameters<?, ?> that = (SafePbeParameters<?, ?>) obj;
        return Objects.equals(this.keyStrength, that.keyStrength)
                && this.changeRequested == that.changeRequested;
    }

    /**
     * Returns a hash code which is consistent with {@link #equals(Object)}.
     */
    @Override
    public final int hashCode() {
        int c = super.hashCode();
        c = 31 * c + Objects.hashCode(keyStrength);
        c = 31 * c + Boolean.valueOf(changeRequested).hashCode();
        return c;
    }
}
