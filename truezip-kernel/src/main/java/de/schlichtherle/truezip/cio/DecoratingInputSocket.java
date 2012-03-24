/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.cio;

import javax.annotation.concurrent.NotThreadSafe;

/**
 * An abstract decorator for an input socket.
 * 
 * @see    DecoratingOutputSocket
 * @param  <E> the type of the {@link #getLocalTarget() local target}.
 * @author Christian Schlichtherle
 */
@NotThreadSafe
public abstract class DecoratingInputSocket<E extends Entry>
extends DelegatingInputSocket<E> {

    private final InputSocket<? extends E> delegate;

    protected DecoratingInputSocket(final InputSocket<? extends E> delegate) {
        if (null == (this.delegate = delegate))
            throw new NullPointerException();
    }

    // TODO: Either declare throws IOException or declare final and declare the
    // delegate field protected final for symmetry with other Decorating*
    // classes.
    @Override
    protected InputSocket<? extends E> getDelegate() {
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
