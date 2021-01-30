/*
 * Copyright (C) 2005-2015 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truecommons3.key.spec;

import net.java.truecommons3.shed.Option;

import javax.annotation.Nullable;
import java.beans.Transient;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.nio.ByteBuffer;
import java.util.Objects;

import static net.java.truecommons3.shed.Buffers.copy;
import static net.java.truecommons3.shed.Buffers.fill;

/**
 * A JavaBean with properties for secret key management.
 * <p>
 * Subclasses need to be serializable with {@code Object(Out|In)putStream} and
 * {@code XML(En|De)coder}.
 * Subclasses do not need to be safe for multi-threading.
 *
 * @param  <K> the type of this safe key.
 * @since TrueCommons 2.2
 * @author Christian Schlichtherle
 */
public abstract class AbstractSecretKey<K extends AbstractSecretKey<K>>
extends AbstractKey<K> implements SecretKey<K> {

    private transient Option<ByteBuffer> secret = Option.none();

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        secret = Option.none();
    }

    @Override
    @SuppressWarnings({"AccessingNonPublicFieldOfAnotherObject", "unchecked"})
    public K clone() {
        final AbstractSecretKey<K> clone = super.clone();
        clone.secret = Option.apply(copy(this.secret.orNull()));
        return (K) clone;
    }

    @Override
    public void reset() { doReset(); }

    private void doReset() {
        for (ByteBuffer bb : secret)
            fill(bb, (byte) 0);
        secret = Option.none();
    }

    @Override
    @SuppressWarnings("FinalizeDeclaration")
    protected void finalize() throws Throwable {
        try { doReset(); }
        finally { super.finalize(); }
    }

    /**
     * Returns {@code true} if and only if the secret data is not {@code null}.
     */
    @Transient
    public final boolean isSecretSet() { return !secret.isEmpty(); }

    @Transient
    @Override
    public final @Nullable ByteBuffer getSecret() {
        return copy(secret.orNull());
    }

    @Override
    public final void setSecret(final @Nullable ByteBuffer secret) {
        doReset();
        this.secret = Option.apply(copy(secret));
    }

    @Override
    @SuppressWarnings("AccessingNonPublicFieldOfAnotherObject")
    public boolean equals(final @Nullable Object obj) {
        if (this == obj)
            return true;
        if (!super.equals(obj))
            return false;
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
