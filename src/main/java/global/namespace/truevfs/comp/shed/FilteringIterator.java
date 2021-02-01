/*
 * Copyright (C) 2005-2015 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package global.namespace.truevfs.comp.shed;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;

/**
 * An iterator which filters another iterator by means of its
 * {@link #accept(Object)} method.
 *
 * @param <T> The type of elements returned by this iterator.
 * @author Christian Schlichtherle
 */
@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
public abstract class FilteringIterator<T> implements Iterator<T> {

    private final Iterator<T> it;
    private Optional<Boolean> hasNext = Optional.empty();
    private Optional<T> next = Optional.empty();

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
     * @param item the nullable item to test.
     * @return {@code true} if and only if this filtering iterator accepts the
     * given element.
     */
    protected abstract boolean accept(T item);

    @Override
    public boolean hasNext() {
        if (hasNext.isPresent()) {
            return hasNext.get();
        }
        while (it.hasNext()) {
            if (accept((next = Optional.of(it.next())).get())) {
                return (hasNext = Optional.of(true)).get();
            }
        }
        return (hasNext = Optional.of(false)).get();
    }

    @Override
    public T next() {
        if (hasNext()) {
            hasNext = Optional.empty(); // consume
            return next.get();
        }
        throw new NoSuchElementException();
    }

    @Override
    public void remove() {
        it.remove();
    }
}
