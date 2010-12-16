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

/**
 * A decorator for file system entries.
 *
 * @param <E> The type of the decorated entry.
 * @author Christian Schlichtherle
 * @version $Id$
 */
public abstract class FilterEntry<E extends Entry>
implements Entry {

    /** The decorated entry. */
    protected final E entry;

    protected FilterEntry(final E entry) {
        if (null == entry)
            throw new NullPointerException();
        this.entry = entry;
    }

    @Override
    public String getName() {
        return entry.getName();
    }

    @Override
    public Type getType() {
        return entry.getType();
    }

    @Override
    public long getSize(Size type) {
        return entry.getSize(type);
    }

    @Override
    public long getTime(Access type) {
        return entry.getTime(type);
    }
}
