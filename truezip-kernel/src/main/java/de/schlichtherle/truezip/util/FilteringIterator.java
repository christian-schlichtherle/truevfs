/*
 * Copyright 2004-2012 Schlichtherle IT Services
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package de.schlichtherle.truezip.util;

import java.util.Iterator;
import java.util.NoSuchElementException;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;

/**
 * An iterator which filters another iterator by means of its
 * {@link #accept(Object)} method.
 * 
 * @param   <T> The type of elements returned by this iterator.
 * @author  Christian Schlichtherle
 * @version $Id$
 */
@NotThreadSafe
public abstract class FilteringIterator<T> implements Iterator<T> {
    private final Iterator<T> it;
    private @CheckForNull Boolean hasNext;
    private @Nullable T next;

    /**
     * Constructs a new filtering iterator which filters the given iterable.
     * 
     * @param iterable the iterable to filter.
     */
    protected FilteringIterator(Iterable<T> iterable) {
        this(iterable.iterator());
    }

    /**
     * Constructs a new filtering iterator which filters the given iterator.
     * 
     * @param iterator the iterator to filter.
     */
    protected FilteringIterator(final Iterator<T> iterator) {
        if (null == (this.it = iterator))
            throw new NullPointerException();
    }

    /**
     * Returns {@code true} if and only if this filtering iterator accepts the
     * given element.
     * 
     * @param  element the element to test
     * @return {@code true} if and only if this filtering iterator accepts the
     *         given element.
     */
    protected abstract boolean accept(@Nullable T element);

    @Override
    public boolean hasNext() {
        if (null != hasNext)
            return hasNext;
        while (it.hasNext())
            if (accept(next = it.next()))
                return hasNext = true;
        return hasNext = false;
    }

    @Override
    public @Nullable T next() {
        if (!hasNext())
            throw new NoSuchElementException();
        hasNext = null; // consume
        return next;
    }

    @Override
    public void remove() {
        it.remove();
    }
}
