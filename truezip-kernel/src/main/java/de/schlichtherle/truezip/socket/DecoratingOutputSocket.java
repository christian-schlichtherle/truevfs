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
 * An abstract decorator for an output socket.
 * 
 * @see     DecoratingInputSocket
 * @param   <E> The type of the {@link #getLocalTarget() local target}.
 * @author  Christian Schlichtherle
 * @version $Id$
 */
@NotThreadSafe
public abstract class DecoratingOutputSocket<E extends Entry>
extends DelegatingOutputSocket<E> {

    private final OutputSocket<? extends E> delegate;

    protected DecoratingOutputSocket(final OutputSocket<? extends E> delegate) {
        if (null == delegate)
            throw new NullPointerException();
        this.delegate = delegate;
    }

    @Override
    protected final OutputSocket<? extends E> getDelegate() {
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
