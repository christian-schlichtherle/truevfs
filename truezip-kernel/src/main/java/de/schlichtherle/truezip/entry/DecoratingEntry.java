/*
 * Copyright 2007-2011 Schlichtherle IT Services
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
package de.schlichtherle.truezip.entry;

import edu.umd.cs.findbugs.annotations.DefaultAnnotation;
import edu.umd.cs.findbugs.annotations.NonNull;
import net.jcip.annotations.ThreadSafe;

/**
 * An abstract decorator for an entry.
 *
 * @param   <E> The type of the decorated entries.
 * @author  Christian Schlichtherle
 * @version $Id$
 */
@ThreadSafe
@DefaultAnnotation(NonNull.class)
public abstract class DecoratingEntry<E extends Entry>
implements Entry {

    /** The decorated entry. */
    protected final E delegate;

    /**
     * Constructs a new decorating file system entry.
     *
     * @param entry the decorated entry.
     */
    protected DecoratingEntry(final E entry) {
        if (null == entry)
            throw new NullPointerException();
        this.delegate = entry;
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
        return new StringBuilder()
                .append(getClass().getName())
                .append("[delegate=")
                .append(delegate)
                .append(']')
                .toString();
    }
}
