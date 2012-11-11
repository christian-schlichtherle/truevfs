/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.key.spec.prompting;

import javax.annotation.concurrent.NotThreadSafe;
import net.java.truevfs.key.spec.safe.*;

/**
 * A JavaBean which holds parameters for password based encryption.
 * <p>
 * Subclasses need to be serializable with {@code Object(Out|In)putStream} and
 * {@code XML(En|De)coder}.
 * Subclasses do not need to be safe for multi-threading.
 *
 * @param  <P> the type of these prompting PBE parameters.
 * @param  <S> the type of the key strength.
 * @author Christian Schlichtherle
 */
@NotThreadSafe
public abstract class PromptingPbeParameters<
        P extends PromptingPbeParameters<P, S>,
        S extends KeyStrength>
extends SafePbeParameters<P, S> implements PromptingKey<P> {

    private boolean changeRequested;

    @Override
    public void reset() {
        super.reset();
        changeRequested = false;
    }

    @Override
    public boolean isChangeRequested() { return changeRequested; }

    @Override
    public void setChangeRequested(final boolean changeRequested) {
        this.changeRequested = changeRequested;
    }

    /**
     * Prompting PBE parameters equal another object if and only if the other
     * object has the same runtime class and their properties compare equal.
     */
    @Override
    @SuppressWarnings("AccessingNonPublicFieldOfAnotherObject")
    public boolean equals(final Object obj) {
        if (this == obj) return true;
        if (!super.equals(obj)) return false;
        final PromptingPbeParameters<?, ?> that = (PromptingPbeParameters<?, ?>) obj;
        return this.changeRequested == that.changeRequested;
    }

    /**
     * Returns a hash code which is consistent with {@link #equals(Object)}.
     */
    @Override
    public int hashCode() {
        int c = super.hashCode();
        c = 31 * c + Boolean.valueOf(changeRequested).hashCode();
        return c;
    }

    @Override
    public String toString() {
        return String.format("%s[changeRequested=%b]",
                super.toString(), changeRequested);
    }
}
