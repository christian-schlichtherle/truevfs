/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.truevfs.kernel.util;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Objects;
import javax.annotation.concurrent.NotThreadSafe;

/**
 * Concatenates two iterators.
 *
 * @param  <E> the type of the iterated elements.
 * @author Christian Schlichtherle
 */
@NotThreadSafe
public final class JointIterator<E> implements Iterator<E> {
    private Iterator<? extends E> i1, i2;

    /**
     * Constructs a new {@code JointIterator} from the given iterators.
     *
     * @param  i1 the first iterator.
     * @param  i2 the second iterator.
     */
    public JointIterator(
            final Iterator<? extends E> i1,
            final Iterator<? extends E> i2) {
        this.i1 = Objects.requireNonNull(i1);
        this.i2 = Objects.requireNonNull(i2);
    }

    /**
     * Constructs a new {@code JointIterator} from the given iterables.
     *
     * @param  i1 the first iterable.
     * @param  i2 the second iterable.
     */
    public JointIterator(
            final Iterable<? extends E> i1,
            final Iterable<? extends E> i2) {
        this.i1 = i1.iterator();
        this.i2 = i2.iterator();
    }

    @Override
    public boolean hasNext() {
        return i1.hasNext()
           || (i1 != i2 && (i1 = i2).hasNext());
    }

    @Override
    public E next() {
        try {
            return i1.next();
        } catch (NoSuchElementException ex) {
            if (i1 == i2)
                throw ex;
            return (i1 = i2).next();
        }
    }

    @Override
    public void remove() {
        i1.remove();
    }
}
