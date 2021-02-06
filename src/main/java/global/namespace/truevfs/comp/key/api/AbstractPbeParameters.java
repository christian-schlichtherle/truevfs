/*
 * Copyright (C) 2005-2015 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package global.namespace.truevfs.comp.key.api;

import global.namespace.truevfs.comp.util.Buffers;
import lombok.val;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import java.beans.Transient;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.Optional;

/**
 * A JavaBean with properties for password based encryption (PBE).
 * Passwords get encoded to and decoded from the underlying secret byte buffer
 * using {@link StandardCharsets#UTF_8}.
 * <p>
 * Subclasses need to be serializable with {@code Object(Out|In)putStream} and {@code XML(En|De)coder}.
 * <p>
 * Subclasses do not need to be thread-safe.
 *
 * @param <P> the type of these PBE parameters.
 * @param <S> the type of the key strength.
 * @author Christian Schlichtherle
 */
public abstract class AbstractPbeParameters<P extends AbstractPbeParameters<P, S>, S extends KeyStrength>
        extends AbstractSecretKey<P> implements PbeParameters<P, S> {

    @CheckForNull
    private S keyStrength;

    @Override
    public void reset() {
        super.reset();
        keyStrength = null;
    }

    @Transient
    @Override
    public char[] getPassword() {
        return Buffers.charArray(getSecret());
    }

    @Override
    public void setPassword(final char[] password) {
        setSecret(Buffers.byteBuffer(password));
    }

    @Transient
    @Override
    public S getKeyStrength() {
        return Optional.ofNullable(keyStrength).orElseThrow(IllegalStateException::new);
    }

    @Override
    public void setKeyStrength(S keyStrength) {
        this.keyStrength = Objects.requireNonNull(keyStrength);
    }

    /**
     * Returns the cipher key strength in bits.
     */
    public int getKeyStrengthBits() {
        return Optional.ofNullable(keyStrength).map(S::getBits).orElseThrow(IllegalStateException::new);
    }

    /**
     * Sets the cipher key strength in bits.
     * Note that this method performs a linear search for the keystrength
     * object, so it should not get used on a regular basis - it's actually
     * only provided to support {@code java.beans.XMLEncoder}.
     *
     * @param bits the cipher key strength in bits.
     * @throws IllegalArgumentException if an unknown bit size is provided.
     * @see #getAllKeyStrengths()
     */
    public void setKeyStrengthBits(final int bits) {
        if (0 == bits) {
            this.keyStrength = null;
        } else {
            for (val s : getAllKeyStrengths()) {
                if (s.getBits() == bits) {
                    this.keyStrength = s;
                    return;
                }
            }
            throw new IllegalArgumentException();
        }
    }

    @Override
    public boolean equals(final @Nullable Object obj) {
        if (this == obj) {
            return true;
        }
        if (!super.equals(obj)) {
            return false;
        }
        val that = (AbstractPbeParameters<?, ?>) obj;
        return Objects.equals(this.getKeyStrength(), that.getKeyStrength());
    }

    @Override
    public int hashCode() {
        int c = super.hashCode();
        c = 31 * c + Objects.requireNonNull(getKeyStrength()).hashCode();
        return c;
    }
}
