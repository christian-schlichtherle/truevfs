/*
 * Copyright 2004-2012 Schlichtherle IT Services
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package de.schlichtherle.truezip.entry;

import javax.annotation.CheckForNull;
import java.util.Iterator;

/**
 * An abstract decorator for an entry container.
 *
 * @param   <E> The type of the entries.
 * @author  Christian Schlichtherle
 * @version $Id$
 */
public abstract class DecoratingEntryContainer<
        E extends Entry,
        C extends EntryContainer<E>>
implements EntryContainer<E> {

    /** The decorated entry container. */
    protected final C delegate;

    /**
     * Constructs a new filter entry container.
     *
     * @param  container the non-{@code null} container to be decorated.
     * @throws NullPointerException iff {@code container} is {@code null}.
     */
    protected DecoratingEntryContainer(final C container) {
        if (null == container)
            throw new NullPointerException();
        this.delegate = container;
    }

    @Override
    public int getSize() {
        return delegate.getSize();
    }

    @Override
    public Iterator<E> iterator() {
        return delegate.iterator();
    }

    @Override
    public @CheckForNull E getEntry(String name) {
        return delegate.getEntry(name);
    }

    /**
     * Returns a string representation of this object for debugging and logging
     * purposes.
     */
    @Override
    public String toString() {
        return new StringBuilder()
                .append(getClass().getName())
                .append("[delegate=")
                .append(delegate)
                .append(']')
                .toString();
    }
}
