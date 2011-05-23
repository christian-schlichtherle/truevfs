/*
 * Copyright (C) 2011 Schlichtherle IT Services
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
package de.schlichtherle.truezip.fs;

import de.schlichtherle.truezip.entry.Entry;
import edu.umd.cs.findbugs.annotations.DefaultAnnotation;
import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import java.util.Iterator;
import java.util.Set;

/**
 * An abstract file system entry is an entry which can implement multiple types
 * and list directory members.
 *
 * @author  Christian Schlichtherle
 * @version $Id$
 */
@DefaultAnnotation(NonNull.class)
public abstract class FsEntry implements Entry {

    /**
     * Returns a string representation of the
     * {@link FsEntryName file system entry name}.
     *
     * @return A string representation of the
     *         {@link FsEntryName file system entry name}.
     */
    @Override
    public abstract String getName();

    /**
     * Returns a set of types implemented by this entry.
     * Whether or not modifying the returned set is supported and the effect
     * on the file system is implementation specific.
     * <p>
     * Some file system types allow an entry to implement multiple entry types.
     * For example, a ZIP or TAR file may contain a file entry with the name
     * {@code foo} and a directory entry with the name {@code foo/}.
     * Yes, this is strange, but shit happens!
     * In this case then, a virtual file system should collapse this into one
     * file system entry which returns {@code true} for both
     * {@code isType(FILE)} and {@code isType(DIRECTORY)}.
     * 
     * @return An unmodifiable set of types implemented by this entry.
     */
    public abstract Set<Type> getTypes();

    /**
     * Returns {@code true} if and only if this file system entry implements
     * the given type.
     *
     * @param  type the type to test.
     * @return {@code true} if and only if this file system entry implements
     *         the given type.
     * @see    #getTypes()
     */
    public boolean isType(Type type) {
        return getTypes().contains(type);
    }

    /**
     * If this is not a directory entry, {@code null} is returned.
     * Otherwise, a set of strings is returned which represent the base names
     * of the members of this directory entry.
     * Whether or not modifying the returned set is supported and the effect
     * on the file system is implementation specific.
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
     * Returns a string representation of this object for debugging and logging
     * purposes.
     */
    @Override
    public String toString() {
        final StringBuilder s = new StringBuilder(getClass().getName())
                .append("[name=").append(getName())
                .append(",types=");//.append(BitField.copyOf(getTypes()));
        for (Iterator<Type> i = getTypes().iterator(); i.hasNext(); ) {
            s.append(i.next());
            if (i.hasNext())
                s.append('|');
        }
        for (Size type : ALL_SIZE_SET)
            s.append(",size(").append(type).append(")=").append(getSize(type));
        for (Access type : ALL_ACCESS_SET)
            s.append(",time(").append(type).append(")=").append(getTime(type));
        return s.append(",members=").append(getMembers()).append(']')
                .toString();
    }
}
