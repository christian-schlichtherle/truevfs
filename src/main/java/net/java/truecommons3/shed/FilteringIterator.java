/*
 * Copyright (C) 2005-2015 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truecommons3.shed;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Objects;

/**
 * An iterator which filters another iterator by means of its
 * {@link #accept(Object)} method.
 * 
 * @param   <T> The type of elements returned by this iterator.
 * @author  Christian Schlichtherle
 */
@SuppressWarnings("LoopStatementThatDoesntLoop")
public abstract class FilteringIterator<T> implements Iterator<T> {

    private final Iterator<T> it;
    private Option<Boolean> hasNext = Option.none();
    private Option<T> next = Option.none();

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
        this.it = Objects.requireNonNull(iterator);
    }

    /**
     * Returns {@code true} if and only if this filtering iterator accepts the
     * given nullable item.
     * 
     * @param  item the nullable item to test.
     * @return {@code true} if and only if this filtering iterator accepts the
     *         given element.
     */
    protected abstract boolean accept(T item);

    @Override
    public boolean hasNext() {
        for (final Boolean b : hasNext)
            return b;
        while (it.hasNext())
            if (accept((next = Option.some(it.next())).get()))
                return (hasNext = Option.some(true)).get();
        return (hasNext = Option.some(false)).get();
    }

    @Override
    public T next() {
        if (hasNext()) {
            hasNext = Option.none(); // consume
            return next.get();
        }
        throw new NoSuchElementException();
    }

    @Override
    public void remove() { it.remove(); }
}