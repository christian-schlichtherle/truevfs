/*
 * Copyright (C) 2005-2015 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truecommons3.shed;

import java.util.Iterator;
import java.util.NoSuchElementException;

import static java.util.Objects.requireNonNull;

/**
 * Concatenates two iterators.
 *
 * @param  <E> the type of the iterated elements.
 * @author Christian Schlichtherle
 */
public final class CompoundIterator<E> implements Iterator<E> {
    private Iterator<? extends E> first, second;

    public CompoundIterator(final Iterator<? extends E> first, final Iterator<? extends E> second) {
        this.first = requireNonNull(first);
        this.second = requireNonNull(second);
    }

    @Override
    public boolean hasNext() {
        return first.hasNext() || (first != second && (first = second).hasNext());
    }

    @Override
    public E next() {
        try {
            return first.next();
        } catch (NoSuchElementException ex) {
            if (first == second) throw ex;
            return (first = second).next();
        }
    }

    @Override
    public void remove() {
        first.remove();
    }
}
