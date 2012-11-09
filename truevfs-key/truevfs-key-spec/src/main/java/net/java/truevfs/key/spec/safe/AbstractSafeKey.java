/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.key.spec.safe;

import java.beans.Transient;
import java.io.Serializable;
import java.nio.ByteBuffer;
import java.util.Objects;
import javax.annotation.CheckForNull;
import static net.java.truevfs.key.spec.util.BufferUtils.*;

/**
 * A safe key for writing and reading protected resources.
 * <p>
 * Subclasses need to be serializable with {@code Object(Out|In)putStream} and
 * {@code XML(En|De)coder}.
 * Subclasses do not need to be safe for multi-threading.
 *
 * @param  <K> the type of this safe key.
 * @author Christian Schlichtherle
 */
public abstract class AbstractSafeKey<K extends AbstractSafeKey<K>>
implements SafeKey<K>, Serializable {

    private transient @CheckForNull ByteBuffer secret;

    private boolean invariants() {
        final ByteBuffer buffer = this.secret;
        if (null != buffer) {
            assert 0 == buffer.position();
            assert buffer.limit() == buffer.capacity();
        }
        return true;
    }

    @Override
    @SuppressWarnings("AccessingNonPublicFieldOfAnotherObject")
    public K clone() {
        try {
            final AbstractSafeKey<K> clone;
            try {
                 clone = (AbstractSafeKey<K>) super.clone();
            } catch (CloneNotSupportedException ex) {
                throw new AssertionError(ex);
            }
            clone.secret = copy(this.secret);
            return (K) clone;
        } finally {
            assert invariants();
        }
    }

    @Override
    public void reset() {
        try {
            fill(secret, (byte) 0);
            secret = null;
        } finally {
            assert invariants();
        }
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
    public final boolean isSecretSet() {
        return null != secret;
    }

    /** Returns a protective copy of the secret data. */
    @Transient
    public final @CheckForNull ByteBuffer getSecret() {
        try {
            return copy(secret);
        } finally {
            assert invariants();
        }
    }

    /**
     * Clears the current secret and sets it to a protective copy of the given
     * secret data.
     *
     * @param secret the secret data to copy and set.
     */
    public final void setSecret(final @CheckForNull ByteBuffer secret) {
        try {
            fill(this.secret, (byte) 0);
            this.secret = copy(secret);
        } finally {
            assert invariants();
        }
    }

    @Override
    @SuppressWarnings("AccessingNonPublicFieldOfAnotherObject")
    public boolean equals(final Object obj) {
        try {
            if (this == obj) return true;
            if (null == obj || !this.getClass().equals(obj.getClass()))
                return false;
            final AbstractSafeKey<?> that = (AbstractSafeKey<?>) obj;
            return Objects.equals(this.secret, that.secret);
        } finally {
            assert invariants();
        }
    }

    @Override
    public int hashCode() {
        try {
            int c = 17;
            c = 31 * c + Objects.hashCode(secret);
            return c;
        } finally {
            assert invariants();
        }
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
