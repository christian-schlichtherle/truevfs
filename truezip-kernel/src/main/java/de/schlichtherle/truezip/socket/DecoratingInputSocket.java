/*
 * Copyright 2004-2012 Schlichtherle IT Services
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package de.schlichtherle.truezip.socket;

import de.schlichtherle.truezip.entry.Entry;
import javax.annotation.concurrent.NotThreadSafe;

/**
 * An abstract decorator for an input socket.
 * 
 * @see     DecoratingOutputSocket
 * @param   <E> The type of the {@link #getLocalTarget() local target}.
 * @author  Christian Schlichtherle
 * @version $Id$
 */
@NotThreadSafe
public abstract class DecoratingInputSocket<E extends Entry>
extends DelegatingInputSocket<E> {

    private final InputSocket<? extends E> delegate;

    protected DecoratingInputSocket(final InputSocket<? extends E> delegate) {
        if (null == delegate)
            throw new NullPointerException();
        this.delegate = delegate;
    }

    @Override
    protected final InputSocket<? extends E> getDelegate() {
        return delegate;
    }

    /**
     * Returns a string representation of this object for debugging and logging
     * purposes.
     */
    @Override
    public String toString() {
        final String n = getClass().getName();
        final String d = delegate.toString();
        return new StringBuilder(n.length() + "[delegate=".length() + d.length() + 1)
                .append(n)
                .append("[delegate=")
                .append(d)
                .append(']')
                .toString();
    }
}
