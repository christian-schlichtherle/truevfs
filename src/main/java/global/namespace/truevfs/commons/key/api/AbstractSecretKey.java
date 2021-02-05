/*
 * Copyright (C) 2005-2015 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package global.namespace.truevfs.commons.key.api;

import global.namespace.truevfs.commons.shed.Buffers;

import javax.annotation.Nullable;
import java.beans.Transient;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.nio.ByteBuffer;
import java.util.Objects;
import java.util.Optional;

import static global.namespace.truevfs.commons.shed.Buffers.fill;

/**
 * A JavaBean with properties for secret key management.
 * <p>
 * Subclasses need to be serializable with {@code Object(Out|In)putStream} and {@code XML(En|De)coder}.
 * <p>
 * Subclasses do not need to be thread-safe.
 *
 * @param  <K> the type of this safe key.
 * @author Christian Schlichtherle
 */
@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
public abstract class AbstractSecretKey<K extends AbstractSecretKey<K>>
extends AbstractKey<K> implements SecretKey<K> {

    private transient Optional<ByteBuffer> optSecret = Optional.empty();

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        optSecret = Optional.empty();
    }

    @SuppressWarnings("unchecked")
    @Override
    public K clone() {
        final AbstractSecretKey<K> clone = super.clone();
        clone.optSecret = this.optSecret.map(Buffers::copy);
        return (K) clone;
    }

    @Override
    public void reset() { doReset(); }

    private void doReset() {
        optSecret.ifPresent(bb -> fill(bb, (byte) 0));
        optSecret = Optional.empty();
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
    public final boolean isSecretSet() { return optSecret.isPresent(); }

    @Transient
    @Override
    public final @Nullable ByteBuffer getSecret() {
        return optSecret.map(Buffers::copy).orElse(null);
    }

    @Override
    public final void setSecret(final @Nullable ByteBuffer secret) {
        doReset();
        this.optSecret = Optional.ofNullable(secret).map(Buffers::copy);
    }

    @Override
    @SuppressWarnings("AccessingNonPublicFieldOfAnotherObject")
    public boolean equals(final @Nullable Object obj) {
        if (this == obj) {
            return true;
        }
        if (!super.equals(obj)) {
            return false;
        }
        final AbstractSecretKey<?> that = (AbstractSecretKey<?>) obj;
        return Objects.equals(this.optSecret, that.optSecret);
    }

    @Override
    public int hashCode() {
        int c = 17;
        c = 31 * c + Objects.hashCode(optSecret);
        return c;
    }

    /**
     * Returns a string representation of this object for logging and debugging
     * purposes.
     */
    @Override
    public String toString() {
        return String.format("%s[secretSet=%b]", super.toString(), isSecretSet());
    }
}
