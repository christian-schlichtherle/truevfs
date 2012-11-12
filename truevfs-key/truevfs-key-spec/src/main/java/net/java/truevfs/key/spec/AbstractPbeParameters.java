/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.key.spec;

import java.beans.Transient;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import static net.java.truevfs.key.spec.util.Buffers.*;

/**
 * A JavaBean with properties for password based encryption (PBE).
 * Passwords get encoded to and decoded from the underlying secret byte buffer
 * using {@link StandardCharsets#UTF_8}.
 * <p>
 * Subclasses need to be serializable with {@code Object(Out|In)putStream} and
 * {@code XML(En|De)coder}.
 * Subclasses do not need to be safe for multi-threading.
 *
 * @param  <P> the type of these PBE parameters.
 * @param  <S> the type of the key strength.
 * @author Christian Schlichtherle
 */
@NotThreadSafe
public abstract class AbstractPbeParameters<
        P extends AbstractPbeParameters<P, S>,
        S extends KeyStrength>
extends AbstractSecretKey<P>
implements PbeParameters<P, S> {

    private @CheckForNull S keyStrength;

    @Override
    public void reset() {
        super.reset();
        keyStrength = null;
    }

    @Transient
    @Override
    public @Nullable char[] getPassword() { return charArray(getSecret()); }

    @Override
    public void setPassword(final @CheckForNull char[] password) {
        setSecret(byteBuffer(password));
    }

    @Transient
    @Override
    public @CheckForNull S getKeyStrength() { return keyStrength; }

    @Override
    public void setKeyStrength(final @CheckForNull S keyStrength) {
        this.keyStrength = keyStrength;
    }

    /** Returns the cipher key strength in bits. */
    public int getKeyStrengthBits() {
        return null == keyStrength ? 0 : keyStrength.getBits();
    }

    /**
     * Sets the cipher key strength in bits.
     * Note that this method performs a linear search for the keystrength
     * object, so it should not get used on a regular basis - it's actually
     * only provided to support {@code java.beans.XMLEncoder}.
     *
     * @param  bits the cipher key strength in bits.
     * @throws IllegalArgumentException if an unknown bit size is provided.
     * @see    #getAllKeyStrengths()
     */
    public void setKeyStrengthBits(final int bits) {
        if (0 == bits) {
            this.keyStrength = null;
        } else {
            for (final S s : getAllKeyStrengths()) {
                if (s.getBits() == bits) {
                    this.keyStrength = s;
                    return;
                }
            }
            throw new IllegalArgumentException();
        }
    }

    @Override
    @SuppressWarnings("AccessingNonPublicFieldOfAnotherObject")
    public boolean equals(final Object obj) {
        if (this == obj) return true;
        if (!super.equals(obj)) return false;
        final AbstractPbeParameters<?, ?> that = (AbstractPbeParameters<?, ?>) obj;
        return Objects.equals(this.keyStrength, that.keyStrength);
    }

    @Override
    public int hashCode() {
        int c = super.hashCode();
        c = 31 * c + Objects.hashCode(keyStrength);
        return c;
    }

    @Override
    public String toString() {
        return String.format("%s[keystrength=%s]",
                super.toString(), keyStrength);
    }
}
