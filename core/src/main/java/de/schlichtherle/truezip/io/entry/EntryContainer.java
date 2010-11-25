/*
 * Copyright 2006-2010 Schlichtherle IT Services
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

package de.schlichtherle.truezip.io.entry;

import java.util.Iterator;

/**
 * An iterable container for entries.
 * <p>
 * Implementations do <em>not</em> need to be thread-safe:
 * Multithreading needs to be addressed by client applications.
 *
 * @param <CE> The type of the entries.
 * @author Christian Schlichtherle
 * @version $Id$
 */
public interface EntryContainer<CE extends Entry>
extends Iterable<CE> {

    /** Returns the number of entries in this container. */
    int size();

    /**
     * Returns a new iterator over all entries in this container.
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
     * @return A new non-{@code null} iterator over all entries in this
     *         container.
     */
    @Override
    Iterator<CE> iterator();

    /**
     * Returns the entry for the given
     * {@link Entry#getName() entry name} or {@code null} if no
     * entry with this name exists in this container.
     *
     * @param  name a non-{@code null} <i>entry name</i>.
     * @return The entry for the given <i>entry name</i> or
     *         {@code null} if no entry with this name exists in this container.
     */
    CE getEntry(String name);
}
