/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.truevfs.kernel.cio;

import java.util.Iterator;
import javax.annotation.CheckForNull;

/**
 * An iterable container for entries.
 *
 * @param  <E> the type of the entries in this container.
 * @author Christian Schlichtherle
 */
public interface Container<E extends Entry> extends Iterable<E> {

    /**
     * Returns the number of entries in this container.
     * 
     * @return The number of entries in this container.
     */
    int size();

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
     * Returns the entry for the given {@code name} or {@code null} if no entry
     * with this name exists in this container.
     *
     * @param  name the name of the entry.
     * @return The entry for the given {@code name} or {@code null} if no entry
     *         with this name exists in this container.
     */
    @CheckForNull E entry(String name);
}
