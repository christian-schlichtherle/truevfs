/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.truezip.kernel.cio;

import java.util.Iterator;
import java.util.Objects;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

/**
 * An abstract decorator for an entry container.
 *
 * @param  <E> the type of the entries in the decorated container.
 * @param  <C> the type of the decorated entry container.
 * @author Christian Schlichtherle
 */
public abstract class DecoratingContainer<
        E extends Entry,
        C extends Container<E>>
implements Container<E> {

    /** The nullable decorated entry container. */
    protected @Nullable C container;

    protected DecoratingContainer() { }

    protected DecoratingContainer(final C container) {
        this.container = Objects.requireNonNull(container);
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
    public @CheckForNull E entry(String name) {
        return container.entry(name);
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
