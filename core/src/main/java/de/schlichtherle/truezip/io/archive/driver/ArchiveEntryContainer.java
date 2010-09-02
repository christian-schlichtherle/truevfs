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

package de.schlichtherle.truezip.io.archive.driver;

import java.util.Iterator;

/**
 * An iterable container for archive entries.
 * <p>
 * Implementations do <em>not</em> need to be thread-safe:
 * Multithreading needs to be addressed by client applications.
 *
 * @param <AE> The type of the archive entries.
 * @author Christian Schlichtherle
 * @version $Id$
 */
public interface ArchiveEntryContainer<AE extends ArchiveEntry>
extends Iterable<AE> {

    /** Returns the number of archive entries in this archive entry container. */
    int size();

    /**
     * Returns a new iteration of all archive entries in this archive entry
     * container.
     * <p>
     * The iteration <em>must</em> be consistent: Multiple iterators must
     * return the same archive entries in the same order again unless the set
     * of entries has changed.
     * <p>
     * The iteration <em>should</em> also reflect the natural order of the
     * entries in this archive entry container.
     * For example, if this archive entry container is an input archive, the
     * iteration should reflect the order of the entries in the archive file.
     * Whether this reflects the order of the entry data in the file or a
     * list of entries in a central directory is up to the implementation.
     *
     * @return An iteration of all archive entries in order
     *         - {@code null} is not permitted.
     */
    Iterator<AE> iterator();

    /**
     * Returns the archive entry for the given archive entry name or
     * {@code null} if no archive entry with this entry name exists in this
     * archive entry container.
     *
     * @param  name a valid archive entry name - never {@code null}.
     * @return The archive entry for the given entry name or {@code null} if
     *         no archive entry with this entry name exists in this archive
     *         entry container.
     * @see <a href="ArchiveEntry.html#entryName">Requirements for Archive Entry Names</a>
     */
    AE getEntry(String name);
}
