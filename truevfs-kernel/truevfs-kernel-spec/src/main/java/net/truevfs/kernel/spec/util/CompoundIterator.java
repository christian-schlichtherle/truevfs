/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.truevfs.kernel.spec.util;

import java.util.Iterator;
import java.util.NoSuchElementException;
import javax.annotation.concurrent.NotThreadSafe;

/**
 * Concatenates two iterators.
 *
 * @param  <E> the type of the iterated elements.
 * @author Christian Schlichtherle
 */
@NotThreadSafe
public final class CompoundIterator<E> implements Iterator<E> {
    private Iterator<? extends E> first, second;

    public CompoundIterator(
            final Iterator<? extends E> first,
            final Iterator<? extends E> second) {
        if (null == (this.first = first)) throw new NullPointerException();
        if (null == (this.second = second)) throw new NullPointerException();
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
