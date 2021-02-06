/*
 * Copyright (C) 2005-2015 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package global.namespace.truevfs.comp.key.api;

import global.namespace.truevfs.comp.util.Buffers;

import javax.annotation.Nullable;
import java.beans.Transient;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.nio.ByteBuffer;
import java.util.Objects;

import static global.namespace.truevfs.comp.util.Buffers.fill;

/**
 * A JavaBean with properties for secret key management.
 * <p>
 * Subclasses need to be serializable with {@code Object(Out|In)putStream} and {@code XML(En|De)coder}.
 * <p>
 * Subclasses do not need to be thread-safe.
 *
 * @param <K> the type of this safe key.
 * @author Christian Schlichtherle
 */
public abstract class AbstractSecretKey<K extends AbstractSecretKey<K>> extends AbstractKey<K> implements SecretKey<K> {

    private transient ByteBuffer secret = ByteBuffer.allocate(0);

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        secret = ByteBuffer.allocate(0);
    }

    @SuppressWarnings("unchecked")
    @Override
    public K clone() {
        final AbstractSecretKey<K> clone = super.clone();
        clone.secret = Buffers.copy(this.secret);
        return (K) clone;
    }

    @Override
    public void reset() {
        doReset();
    }

    private void doReset() {
        fill(secret, (byte) 0);
        secret = ByteBuffer.allocate(0);
    }

    @Override
    @SuppressWarnings("FinalizeDeclaration")
    protected void finalize() throws Throwable {
        try {
            doReset();
        } finally {
            super.finalize();
        }
    }

    @Transient
    @Override
    public final ByteBuffer getSecret() {
        return Buffers.copy(secret);
    }

    @Override
    public final void setSecret(final ByteBuffer secret) {
        doReset();
        this.secret = Buffers.copy(secret);
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
        return Objects.equals(this.secret, that.secret);
    }

    @Override
    public int hashCode() {
        int c = 17;
        c = 31 * c + Objects.hashCode(secret);
        return c;
    }
}
