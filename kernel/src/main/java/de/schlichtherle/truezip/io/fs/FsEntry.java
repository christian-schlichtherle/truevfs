/*
 * Copyright (C) 2010 Schlichtherle IT Services
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
package de.schlichtherle.truezip.io.fs;

import de.schlichtherle.truezip.entry.Entry;
import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import java.util.Set;
import net.jcip.annotations.NotThreadSafe;

/**
 * A file system entry is an entry which can list directory members.
 * Optionally, it may also provide access to another entry which is decorated
 * by it.
 *
 * @author  Christian Schlichtherle
 * @version $Id$
 */
@NotThreadSafe
public abstract class FsEntry implements Entry {

    /**
     * Returns a string representation of the
     * {@link FsEntryName file system entry name}.
     *
     * @return A string representation of the
     *         {@link FsEntryName file system entry name}.
     */
    @Override
    public abstract @NonNull String getName();

    /**
     * If this is not a directory entry, {@code null} is returned.
     * Otherwise, an unmodifiable set of strings is returned which
     * represent the base names of the members of this directory entry.
     */
    public abstract @Nullable Set<String> getMembers();

    /**
     * Two file system entries are considered equal if and only if they are
     * identical.
     * This can't get overriden.
     */
    @Override
    @SuppressWarnings("EqualsWhichDoesntCheckParameterClass")
    public final boolean equals(Object that) {
        return this == that;
    }

    /**
     * Returns a hash code which is consistent with {@link #equals}.
     * This can't get overriden.
     */
    @Override
    public final int hashCode() {
        return super.hashCode();
    }

    /**
     * Returns {@link #getName()}.
     * 
     * @return {@link #getName()}.
     */
    @Override
    public String toString() {
        return getName();
    }
}
