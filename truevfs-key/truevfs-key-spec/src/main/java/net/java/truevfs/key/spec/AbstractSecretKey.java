/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.key.spec;

import java.beans.Transient;
import java.nio.ByteBuffer;
import java.util.Objects;
import javax.annotation.CheckForNull;
import javax.annotation.concurrent.NotThreadSafe;
import static net.java.truevfs.key.spec.util.BufferUtils.*;

/**
 * A JavaBean with properties for secret key management.
 * <p>
 * Subclasses need to be serializable with {@code Object(Out|In)putStream} and
 * {@code XML(En|De)coder}.
 * Subclasses do not need to be safe for multi-threading.
 *
 * @param  <K> the type of this safe key.
 * @author Christian Schlichtherle
 */
@NotThreadSafe
public abstract class AbstractSecretKey<K extends AbstractSecretKey<K>>
extends AbstractKey<K> implements SecretKey<K> {

    private transient @CheckForNull ByteBuffer secret;

    @Override
    @SuppressWarnings("AccessingNonPublicFieldOfAnotherObject")
    public K clone() {
        final AbstractSecretKey<K> clone = super.clone();
        clone.secret = copy(this.secret);
        return (K) clone;
    }

    @Override
    public void reset() {
        fill(secret, (byte) 0);
        secret = null;
    }

    @Override
    @SuppressWarnings("FinalizeDeclaration")
    protected void finalize() throws Throwable {
        try {
            super.finalize();
        } finally {
            fill(secret, (byte) 0);
        }
    }

    /**
     * Returns {@code true} if and only if the secret data is not {@code null}.
     */
    @Transient
    public final boolean isSecretSet() { return null != secret; }

    @Transient
    @Override
    public final @CheckForNull ByteBuffer getSecret() {
        return copy(secret);
    }

    @Override
    public final void setSecret(final @CheckForNull ByteBuffer secret) {
        fill(this.secret, (byte) 0);
        this.secret = copy(secret);
    }

    @Override
    @SuppressWarnings("AccessingNonPublicFieldOfAnotherObject")
    public boolean equals(final Object obj) {
        if (this == obj) return true;
        if (!super.equals(obj)) return false;
        final AbstractSecretKey<?> that = (AbstractSecretKey<?>) obj;
        return Objects.equals(this.secret, that.secret);
    }

    @Override
    public int hashCode() {
        int c = 17;
        c = 31 * c + Objects.hashCode(secret);
        return c;
    }

    /**
     * Returns a string representation of this object for logging and debugging
     * purposes.
     */
    @Override
    public String toString() {
        return String.format("%s[secretSet=%b]",
                super.toString(), isSecretSet());
    }
}
