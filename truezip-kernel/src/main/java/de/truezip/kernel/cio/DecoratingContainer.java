/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.truezip.kernel.cio;

import java.util.Iterator;
import javax.annotation.CheckForNull;

/**
 * An abstract decorator for an entry container.
 *
 * @param  <E> the type of the entries in the container.
 * @param  <C> the type of the entry container.
 * @author Christian Schlichtherle
 */
public abstract class DecoratingContainer<
        E extends Entry,
        C extends Container<E>>
implements Container<E> {

    /** The decorated entry container. */
    protected final C container;

    /**
     * Constructs a new filter entry container.
     *
     * @param container the entry container to decorate.
     */
    protected DecoratingContainer(final C container) {
        if (null == (this.container = container))
            throw new NullPointerException();
    }

    @Override
    public int size() {
        return container.size();
    }

    @Override
    public Iterator<E> iterator() {
        return container.iterator();
    }

    @Override
    public @CheckForNull E getEntry(String name) {
        return container.getEntry(name);
    }

    /**
     * Returns a string representation of this object for debugging and logging
     * purposes.
     */
    @Override
    public String toString() {
        return String.format("%s[container=%s]",
                getClass().getName(),
                container);
    }
}
