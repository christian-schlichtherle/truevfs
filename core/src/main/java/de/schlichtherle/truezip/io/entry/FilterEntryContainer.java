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
package de.schlichtherle.truezip.io.entry;

import java.util.Iterator;

/*
 * Decorates a {@code EntryContainer}.
 * <p>
 * Implementations do <em>not</em> need to be thread-safe:
 * Multithreading needs to be addressed by client applications.
 *
 * @param <CE> The type of the entries.
 * @author Christian Schlichtherle
 * @version $Id$
 */
public abstract class FilterEntryContainer<
        CE  extends Entry,
        CEC extends EntryContainer<CE>>
implements EntryContainer<CE> {

    /** The decorated entry container. */
    protected CEC target;

    protected FilterEntryContainer(final CEC target) {
        this.target = target;
    }

    @Override
    public int size() {
        return target.size();
    }

    @Override
    public Iterator<CE> iterator() {
        return target.iterator();
    }

    @Override
    public CE getEntry(String name) {
        return target.getEntry(name);
    }
}
