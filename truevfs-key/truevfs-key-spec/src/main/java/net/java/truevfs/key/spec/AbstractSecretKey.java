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
 * A secret key for writing and reading protected resources which may be used
 * with {@linkplain PromptingKeyProvider prompting key providers} and their
 * {@linkplain PromptingKeyManager prompting key manager}.
 *
 * @param  <K> the type of this secret key.
 * @author Christian Schlichtherle
 */
public abstract class AbstractSecretKey<K extends AbstractSecretKey<K>>
implements SecretKey<K> {

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

    @Override
    public final @CheckForNull ByteBuffer getSecret() {
        try {
            return copy(secret);
        } finally {
            assert invariants();
        }
    }

    @Override
    public final void setSecret(final @CheckForNull ByteBuffer secret) {
        try {
            fill(this.secret, (byte) 0);
            this.secret = copy(secret);
        } finally {
            assert invariants();
        }
    }

    /**
     * An abstract key equals another object if and only if the other object
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
     * never use secret key classes as keys in hash maps because of their
     * mutable properties!
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
