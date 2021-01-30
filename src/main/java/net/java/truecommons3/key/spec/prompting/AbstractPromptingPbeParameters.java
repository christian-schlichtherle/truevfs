/*
 * Copyright (C) 2005-2015 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truecommons3.key.spec.prompting;

import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import net.java.truecommons3.key.spec.AbstractPbeParameters;
import net.java.truecommons3.key.spec.KeyStrength;

/**
 * A JavaBean with properties for prompting for password based encryption (PBE)
 * parameters.
 * <p>
 * Subclasses need to be serializable with {@code Object(Out|In)putStream} and
 * {@code XML(En|De)coder}.
 * Subclasses do not need to be safe for multi-threading.
 *
 * @param  <P> the type of these prompting PBE parameters.
 * @param  <S> the type of the key strength.
 * @since  TrueCommons 2.2
 * @author Christian Schlichtherle
 */
@NotThreadSafe
public abstract class AbstractPromptingPbeParameters<
        P extends AbstractPromptingPbeParameters<P, S>,
        S extends KeyStrength>
extends AbstractPbeParameters<P, S> implements PromptingPbeParameters<P, S> {

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
    public boolean equals(final @Nullable Object obj) {
        if (this == obj)
            return true;
        if (!super.equals(obj))
            return false;
        final AbstractPromptingPbeParameters<?, ?> that = (AbstractPromptingPbeParameters<?, ?>) obj;
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
