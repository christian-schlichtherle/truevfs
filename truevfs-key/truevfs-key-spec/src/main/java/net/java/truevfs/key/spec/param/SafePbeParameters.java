/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.key.spec.param;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import net.java.truevfs.key.spec.SafeKey;

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

    private @CheckForNull CharBuffer buffer;
    private @CheckForNull S keyStrength;

    private boolean invariants() {
        final CharBuffer buffer = this.buffer;
        if (null != buffer) {
            assert 0 == buffer.position();
            assert buffer.limit() == buffer.capacity();
        }
        return true;
    }

    @Override
    @SuppressWarnings("AccessingNonPublicFieldOfAnotherObject")
    public P clone() {
        try {
            final SafePbeParameters<P, S> clone;
            try {
                 clone = (SafePbeParameters<P, S>) super.clone();
            } catch (CloneNotSupportedException ex) {
                throw new AssertionError(ex);
            }
            clone.setBuffer(this.getBuffer());
            return (P) clone;
        } finally {
            assert invariants();
        }
    }

    @Override
    public void reset() {
        setPassword(null);
        setKeyStrength(null);
    }

    private CharBuffer getBuffer() {
        final CharBuffer buffer = this.buffer;
        return null == buffer ? null : buffer.asReadOnlyBuffer();
    }

    private void setBuffer(final @CheckForNull CharBuffer buffer) {
        this.buffer = null == buffer
                ? null
                : (CharBuffer) ByteBuffer
                    .allocateDirect(2 * buffer.remaining())
                    .asCharBuffer()
                    .put(buffer/*.duplicate()*/)
                    .rewind();
    }

    /**
     * Returns a protective copy of the stored password char array.
     * It's highly recommended to overwrite the char array with any
     * non-password data after using the password.
     *
     * @return A protective copy of the stored password char array.
     */
    public @Nullable char[] getPassword() {
        try {
            final CharBuffer buffer = this.buffer;
            if (null == buffer) return null;
            final char[] password = new char[buffer.remaining()];
            buffer.get(password).rewind();
            return password;
        } finally {
            assert invariants();
        }
    }

    /**
     * Copies and stores the given password char array for deriving the cipher
     * key.
     * It's highly recommended to overwrite the char array with any
     * non-password data after calling this method.
     *
     * @param password the password char array for deriving the cipher key.
     */
    public void setPassword(final @CheckForNull char[] password) {
        try {
            clearBuffer();
            if (null != password) setBuffer(CharBuffer.wrap(password));
        } finally {
            assert invariants();
        }
    }

    private void clearBuffer() {
        final CharBuffer buffer = this.buffer;
        if (null == buffer) return;
        final int remaining = buffer.remaining();
        for (int i = 0; i < remaining; i++) buffer.put((char) 0);
        this.buffer = null;
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
        try {
            // Decode the characters from UTF-16BE, so that the byte order
            // is preserved when the char array is later translated to a byte
            // array again in accordance with PKCS #12, section B.1.
            setBuffer(ByteBuffer.wrap(bytes).asCharBuffer());
        } finally {
            assert invariants();
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
    public @Nullable S getKeyStrength() { return keyStrength; }

    /**
     * Sets the cipher key strength.
     *
     * @param keyStrength the cipher key strength.
     */
    public void setKeyStrength(final @CheckForNull S keyStrength) {
        this.keyStrength = keyStrength;
    }
}
