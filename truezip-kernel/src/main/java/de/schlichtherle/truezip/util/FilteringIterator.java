/*
 * Copyright (C) 2011 Schlichtherle IT Services
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package de.schlichtherle.truezip.util;

import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.DefaultAnnotation;
import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import java.util.Iterator;
import java.util.NoSuchElementException;
import net.jcip.annotations.NotThreadSafe;

/**
 * An iterator which filters another iterator.
 * 
 * @author  Christian Schlichtherle
 * @version $Id$
 */
@NotThreadSafe
@DefaultAnnotation(NonNull.class)
public abstract class FilteringIterator<T> implements Iterator<T> {
    private final Iterator<T> iterator;
    private @CheckForNull Boolean hasNext;
    private @Nullable T next;

    /**
     * Constructs a new filtering iterator which filters the given iterator.
     * 
     * @param iterator the iterator to filter.
     */
    protected FilteringIterator(final Iterator<T> iterator) {
        assert null != iterator; // doesn't matter much if assertions are disabled
        this.iterator = iterator;
    }

    /**
     * Constructs a new filtering iterator which filters the given iterable.
     * 
     * @param iterable the iterable to filter.
     */
    protected FilteringIterator(final Iterable<T> iterable) {
        this.iterator = iterable.iterator();
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
        while (iterator.hasNext())
            if (accept(next = iterator.next()))
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
        iterator.remove();
    }
}
