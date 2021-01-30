/*
 * Copyright (C) 2005-2015 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truecommons.cio;

import edu.umd.cs.findbugs.annotations.DischargesObligation;
import net.java.truecommons.shed.UniqueObject;

import javax.annotation.Nullable;
import java.util.Iterator;
import java.util.Objects;

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
extends UniqueObject implements Container<E> {

    /** The nullable decorated entry container. */
    protected @Nullable C container;

    protected DecoratingContainer() { }

    protected DecoratingContainer(final C container) {
        this.container = Objects.requireNonNull(container);
    }

    @Override
    public int size() { return container.size(); }

    @Override
    public Iterator<E> iterator() { return container.iterator(); }

    @Override
    public @Nullable E entry(String name) { return container.entry(name); }

    @Override
    @DischargesObligation
    public void close() throws Exception { container.close(); }

    /**
     * Returns a string representation of this object for debugging and logging
     * purposes.
     */
    @Override
    public String toString() {
        return String.format("%s@%x[container=%s]",
                getClass().getName(),
                hashCode(),
                container);
    }
}
