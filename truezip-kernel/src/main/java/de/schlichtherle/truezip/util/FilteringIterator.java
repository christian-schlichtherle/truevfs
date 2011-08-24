/*
 * Copyright (C) 2011 Schlichtherle IT Services
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
