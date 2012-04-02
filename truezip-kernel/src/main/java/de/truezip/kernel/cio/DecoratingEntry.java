/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.truezip.kernel.cio;

import javax.annotation.concurrent.ThreadSafe;

/**
 * An abstract decorator for an entry.
 *
 * @param  <E> the type of the decorated entry.
 * @author Christian Schlichtherle
 */
@ThreadSafe
public abstract class DecoratingEntry<E extends Entry>
implements Entry {

    /** The decorated entry. */
    protected final E entry;

    /**
     * Constructs a new decorating file system entry.
     *
     * @param entry the decorated entry.
     */
    protected DecoratingEntry(final E entry) {
        if (null == (this.entry = entry))
            throw new NullPointerException();
    }

    @Override
    public String getName() {
        return entry.getName();
    }

    @Override
    public long getSize(Size type) {
        return entry.getSize(type);
    }

    @Override
    public long getTime(Access type) {
        return entry.getTime(type);
    }

    /**
     * Returns a string representation of this object for debugging and logging
     * purposes.
     */
    @Override
    public String toString() {
        return String.format("%s[delegate=%s]",
                getClass().getName(),
                entry);
    }
}
