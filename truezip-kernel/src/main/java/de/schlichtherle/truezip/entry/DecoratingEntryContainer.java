/*
 * Copyright 2007-2011 Schlichtherle IT Services
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
package de.schlichtherle.truezip.entry;

import java.util.Iterator;
import net.jcip.annotations.ThreadSafe;

/**
 * An abstract decorator for an entry container.
 *
 * @param   <E> The type of the entries.
 * @author  Christian Schlichtherle
 * @version $Id$
 */
@ThreadSafe
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
    public E getEntry(String name) {
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
