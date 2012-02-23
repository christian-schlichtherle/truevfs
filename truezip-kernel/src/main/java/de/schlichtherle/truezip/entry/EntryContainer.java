/*
 * Copyright 2004-2012 Schlichtherle IT Services
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package de.schlichtherle.truezip.entry;

import java.util.Iterator;
import javax.annotation.CheckForNull;

/**
 * An iterable container for entries.
 *
 * @param   <E> the type of the entries in this container.
 * @author  Christian Schlichtherle
 * @version $Id$
 */
public interface EntryContainer<E extends Entry>
extends Iterable<E> {

    /**
     * Returns the number of entries in this container.
     * 
     * @return The number of entries in this container.
     */
    // TODO: Rename this to size().
    int getSize();

    /**
     * Returns a new iterator for all entries in this container.
     * <p>
     * First, the iteration <em>must</em> be consistent: Multiple iterators
     * must iterate the same entries in the same order again unless the set
     * of entries has changed.
     * <p>
     * Next, the iteration <em>should</em> also reflect the natural order of
     * the entries in this container.
     * For example, if this container represents an archive file, the iteration
     * should reflect the natural order of the entries in the archive file.
     *
     * @return A new iterator for all entries in this container.
     */
    @Override
    Iterator<E> iterator();

    /**
     * Returns the entry for the given {@link Entry#getName() name} or
     * {@code null} if no entry with this name exists in this container.
     *
     * @param  name an entry name.
     * @return The entry for the given {@link Entry#getName() name} or
     *         {@code null} if no entry with this name exists in this container.
     */
    @CheckForNull E getEntry(String name);
}
