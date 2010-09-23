/*
 * Copyright 2007-2010 Schlichtherle IT Services
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

package de.schlichtherle.truezip.io.archive.entry;

import java.util.Iterator;

/*
 * Decorates an {@code ArchiveEntryContainer}.
 * <p>
 * Implementations do <em>not</em> need to be thread-safe:
 * Multithreading needs to be addressed by client applications.
 *
 * @param <AE> The type of the archive entries.
 * @author Christian Schlichtherle
 * @version $Id$
 */
public abstract class FilterArchiveEntryContainer<
        AE extends ArchiveEntry,
        AEC extends ArchiveEntryContainer<AE>>
implements ArchiveEntryContainer<AE> {

    /** The decorated archive entry container. */
    protected AEC target;

    protected FilterArchiveEntryContainer(final AEC entry) {
        this.target = entry;
    }

    @Override
    public int size() {
        return target.size();
    }

    @Override
    public Iterator<AE> iterator() {
        return target.iterator();
    }

    @Override
    public AE getEntry(String name) {
        return target.getEntry(name);
    }
}
