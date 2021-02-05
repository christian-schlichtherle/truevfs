/*
 * Copyright (C) 2005-2015 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package global.namespace.truevfs.commons.key.api;

import global.namespace.truevfs.commons.shed.Buffers;

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
@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
public abstract class AbstractPbeParameters<
        P extends AbstractPbeParameters<P, S>,
        S extends KeyStrength>
        extends AbstractSecretKey<P>
        implements PbeParameters<P, S> {

    private Optional<S> keyStrength = Optional.empty();

    @Override
    public void reset() {
        super.reset();
        keyStrength = Optional.empty();
    }

    @Transient
    @Override
    public @Nullable
    char[] getPassword() {
        return Optional.ofNullable(getSecret()).map(Buffers::charArray).orElse(null);
    }

    @Override
    public void setPassword(final @Nullable char[] password) {
        setSecret(Optional.ofNullable(password).map(Buffers::byteBuffer).orElse(null));
    }

    @Transient
    @Override
    public @Nullable
    S getKeyStrength() {
        return keyStrength.orElse(null);
    }

    @Override
    public void setKeyStrength(final @Nullable S keyStrength) {
        this.keyStrength = Optional.ofNullable(keyStrength);
    }

    /**
     * Returns the cipher key strength in bits.
     */
    public int getKeyStrengthBits() {
        return keyStrength.map(S::getBits).orElse(0);
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
            this.keyStrength = Optional.empty();
        } else {
            for (final S s : getAllKeyStrengths()) {
                if (s.getBits() == bits) {
                    this.keyStrength = Optional.of(s);
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
        final AbstractPbeParameters<?, ?> that = (AbstractPbeParameters<?, ?>) obj;
        return Objects.equals(this.getKeyStrength(), that.getKeyStrength());
    }

    @Override
    public int hashCode() {
        int c = super.hashCode();
        c = 31 * c + Objects.requireNonNull(getKeyStrength()).hashCode();
        return c;
    }

    @Override
    public String toString() {
        return String.format("%s[keystrength=%s]", super.toString(), getKeyStrength());
    }
}
