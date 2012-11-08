/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.key.spec;

import java.nio.ByteBuffer;
import java.util.Objects;
import javax.annotation.CheckForNull;
import static net.java.truevfs.key.spec.BufferUtils.*;

/**
 * A secret key for writing and reading protected resources.
 * <p>
 * Subclasses do <em>not</em> need to be safe for multi-threading.
 *
 * @param  <K> the type of this secret key.
 * @author Christian Schlichtherle
 */
public abstract class AbstractSecretKey<K extends AbstractSecretKey<K>>
implements SafeKey<K> {

    private @CheckForNull ByteBuffer secret;

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
            final AbstractSecretKey<K> clone;
            try {
                 clone = (AbstractSecretKey<K>) super.clone();
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

    /**
     * A secret key equals another object if and only if the other object
     * has the same runtime class and its property {@code secret} compares
     * equal.
     */
    @Override
    @SuppressWarnings("AccessingNonPublicFieldOfAnotherObject")
    public boolean equals(final Object obj) {
        try {
            if (this == obj) return true;
            if (null == obj || !this.getClass().equals(obj.getClass()))
                return false;
            final AbstractSecretKey<?> that = (AbstractSecretKey<?>) obj;
            return Objects.equals(this.secret, that.secret);
        } finally {
            assert invariants();
        }
    }

    /**
     * Returns a hash code which is consistent with {@link #equals(Object)}.
     * This method is provided for completeness only - you should actually
     * never use secret keys as hash map keys because of their mutable
     * properties!
     */
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
}
