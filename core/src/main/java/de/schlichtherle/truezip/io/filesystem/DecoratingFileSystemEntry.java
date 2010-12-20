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
package de.schlichtherle.truezip.io.filesystem;

import de.schlichtherle.truezip.io.entry.Entry;
import edu.umd.cs.findbugs.annotations.NonNull;

/**
 * A decorator for file system entries.
 *
 * @param   <E> The type of the decorated entries.
 * @author  Christian Schlichtherle
 * @version $Id$
 */
public abstract class DecoratingFileSystemEntry<E extends Entry>
extends FileSystemEntry {

    /** The decorated entry. */
    @NonNull
    protected final E delegate;

    protected DecoratingFileSystemEntry(@NonNull final E entry) {
        if (null == entry)
            throw new NullPointerException();
        this.delegate = entry;
    }

    @Override
    @NonNull
    public String getName() {
        return delegate.getName();
    }

    @Override
    @NonNull
    public Type getType() {
        return delegate.getType();
    }

    @Override
    public long getSize(Size type) {
        return delegate.getSize(type);
    }

    @Override
    public long getTime(Access type) {
        return delegate.getTime(type);
    }
}
