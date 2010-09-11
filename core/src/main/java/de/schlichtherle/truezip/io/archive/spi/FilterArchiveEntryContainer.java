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

package de.schlichtherle.truezip.io.archive.spi;

import de.schlichtherle.truezip.io.archive.entry.ArchiveEntry;
import de.schlichtherle.truezip.io.archive.entry.ArchiveEntryContainer;
import java.util.Iterator;

/*
 * A decorator for archive entry containers.
 * <p>
 * Implementations do <em>not</em> need to be thread-safe:
 * Multithreading needs to be addressed by client applications.
 *
 * @param <AE> The type of the archive entries.
 * @author Christian Schlichtherle
 * @version $Id$
 */
public abstract class FilterArchiveEntryContainer<AE extends ArchiveEntry>
implements ArchiveEntryContainer<AE> {

    /** Returns the decorated archive entry container. */
    protected abstract ArchiveEntryContainer<AE> getTarget();

    @Override
    public int size() {
        return getTarget().size();
    }

    @Override
    public Iterator<AE> iterator() {
        return getTarget().iterator();
    }

    @Override
    public AE getEntry(String name) {
        return getTarget().getEntry(name);
    }
}
