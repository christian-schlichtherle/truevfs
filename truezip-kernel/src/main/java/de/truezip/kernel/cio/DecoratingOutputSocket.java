/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.truezip.kernel.cio;

import java.io.IOException;
import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;

/**
 * An abstract decorator for an output socket.
 * 
 * @see    DecoratingInputSocket
 * @param  <E> the type of the {@link #getLocalTarget() local target}.
 * @author Christian Schlichtherle
 */
@NotThreadSafe
public abstract class DecoratingOutputSocket<E extends Entry>
extends DelegatingOutputSocket<E> {

    /** The nullable decorated output socket. */
    protected @Nullable OutputSocket<? extends E> delegate;

    protected DecoratingOutputSocket(
            final @Nullable OutputSocket<? extends E> delegate) {
        this.delegate = delegate;
    }

    @Override
    protected OutputSocket<? extends E> getDelegate() throws IOException {
        return delegate;
    }

    /**
     * Returns a string representation of this object for debugging and logging
     * purposes.
     */
    @Override
    public String toString() {
        return String.format("%s[delegate=%s]",
                getClass().getName(),
                delegate);
    }
}
