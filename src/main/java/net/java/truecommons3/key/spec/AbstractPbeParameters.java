/*
 * Copyright (C) 2005-2015 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truecommons3.key.spec;

import net.java.truecommons3.shed.Option;

import java.beans.Transient;
import java.nio.charset.StandardCharsets;
import javax.annotation.Nullable;

import static net.java.truecommons3.shed.Buffers.*;

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
 * @since TrueCommons 2.2
 * @author Christian Schlichtherle
 */
@SuppressWarnings("LoopStatementThatDoesntLoop")
public abstract class AbstractPbeParameters<
        P extends AbstractPbeParameters<P, S>,
        S extends KeyStrength>
extends AbstractSecretKey<P>
implements PbeParameters<P, S> {

    private Option<S> keyStrength = Option.none();

    @Override
    public void reset() {
        super.reset();
        keyStrength = Option.none();
    }

    @Transient
    @Override
    public @Nullable char[] getPassword() { return charArray(getSecret()); }

    @Override
    public void setPassword(final @Nullable char[] password) {
        setSecret(byteBuffer(password));
    }

    @Transient
    @Override
    public @Nullable S getKeyStrength() { return keyStrength.orNull(); }

    @Override
    public void setKeyStrength(final @Nullable S keyStrength) {
        this.keyStrength = Option.apply(keyStrength);
    }

    /** Returns the cipher key strength in bits. */
    public int getKeyStrengthBits() {
        for (S s : keyStrength)
            return s.getBits();
        return 0;
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
            this.keyStrength = Option.none();
        } else {
            for (final S s : getAllKeyStrengths()) {
                if (s.getBits() == bits) {
                    this.keyStrength = Option.some(s);
                    return;
                }
            }
            throw new IllegalArgumentException();
        }
    }

    @Override
    @SuppressWarnings("AccessingNonPublicFieldOfAnotherObject")
    public boolean equals(final @Nullable Object obj) {
        if (this == obj)
            return true;
        if (!super.equals(obj))
            return false;
        final AbstractPbeParameters<?, ?> that = (AbstractPbeParameters<?, ?>) obj;
        return this.keyStrength.equals(that.keyStrength);
    }

    @Override
    public int hashCode() {
        int c = super.hashCode();
        c = 31 * c + keyStrength.hashCode();
        return c;
    }

    @Override
    public String toString() {
        return String.format("%s[keystrength=%s]",
                super.toString(), keyStrength.orNull());
    }
}
