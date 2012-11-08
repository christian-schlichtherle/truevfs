/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.key.spec.safe;

import java.nio.ByteBuffer;
import java.util.Objects;
import javax.annotation.CheckForNull;
import static net.java.truevfs.key.spec.safe.BufferUtils.*;

/**
 * A safe key for writing and reading protected resources.
 * <p>
 * Subclasses do not need to be safe for multi-threading.
 *
 * @param  <K> the type of this safe key.
 * @author Christian Schlichtherle
 */
public abstract class AbstractSafeKey<
        K extends AbstractSafeKey<K, S>,
        S extends SafeKeyStrength>
implements SafeKey<K, S> {

    private @CheckForNull ByteBuffer secret;
    private @CheckForNull S keyStrength;

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
            final AbstractSafeKey<K, S> clone;
            try {
                 clone = (AbstractSafeKey<K, S>) super.clone();
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
            keyStrength = null;
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

    /** Returns a protective copy of the secret. */
    public final @CheckForNull ByteBuffer getSecret() {
        try {
            return copy(secret);
        } finally {
            assert invariants();
        }
    }

    /**
     * Clears the current secret and sets it to a protective copy of the given
     * secret.
     *
     * @param secret the secret to copy and set.
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
    public @CheckForNull S getKeyStrength() {
        try {
            return keyStrength;
        } finally {
            assert invariants();
        }
    }

    @Override
    public void setKeyStrength(final @CheckForNull S keyStrength) {
        try {
            this.keyStrength = keyStrength;
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
            final AbstractSafeKey<?, ?> that = (AbstractSafeKey<?, ?>) obj;
            return Objects.equals(this.secret, that.secret)
                    && Objects.equals(this.keyStrength, that.keyStrength);
        } finally {
            assert invariants();
        }
    }

    @Override
    public int hashCode() {
        try {
            int c = 17;
            c = 31 * c + Objects.hashCode(secret);
            c = 31 * c + Objects.hashCode(keyStrength);
            return c;
        } finally {
            assert invariants();
        }
    }
}
