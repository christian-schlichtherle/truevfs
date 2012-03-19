/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package de.schlichtherle.truezip.fs;

import de.schlichtherle.truezip.entry.Entry;
import javax.annotation.concurrent.ThreadSafe;

/**
 * An abstract decorator for an entry.
 *
 * @param   <E> The type of the decorated entries.
 * @author  Christian Schlichtherle
 * @version $Id$
 */
@ThreadSafe
public abstract class FsDecoratingEntry<E extends Entry> extends FsEntry {

    /** The decorated entry. */
    protected final E delegate;

    /**
     * Constructs a new decorating entry.
     *
     * @param delegate the entry to decorate.
     */
    protected FsDecoratingEntry(final E delegate) {
        if (null == delegate)
            throw new NullPointerException();
        this.delegate = delegate;
    }

    @Override
    public String getName() {
        return delegate.getName();
    }

    @Override
    public long getSize(Size type) {
        return delegate.getSize(type);
    }

    @Override
    public long getTime(Access type) {
        return delegate.getTime(type);
    }

    /**
     * Returns a string representation of this object for debugging and logging
     * purposes.
     */
    @Override
    public String toString() {
        return String.format("%s[delegate=%s]",
                getClass().getName(),
                delegate);
    }
}
