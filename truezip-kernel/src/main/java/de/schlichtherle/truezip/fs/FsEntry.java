/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package de.schlichtherle.truezip.fs;

import de.schlichtherle.truezip.entry.Entry;
import de.schlichtherle.truezip.util.BitField;
import java.util.Formatter;
import java.util.Set;
import javax.annotation.Nullable;

/**
 * An abstract file system entry is an entry which can implement multiple types
 * and list directory members.
 *
 * @author  Christian Schlichtherle
 * @version $Id$
 */
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
     * Returns a set of strings with the base names of the members of this
     * directory entry or {@code null} if and only if this is not a directory
     * entry.
     * Whether or not modifying the returned set is supported and the effect
     * on the file system is implementation specific.
     * 
     * @return A set of strings with the base names of the members of this
     *         directory entry or {@code null} if and only if this is not a
     *         directory entry.
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
        final StringBuilder s = new StringBuilder(256);
        final Formatter f = new Formatter(s).format("%s[name=%s, types=%s",
                getClass().getName(), getName(), BitField.copyOf(getTypes()));
        for (Size type : ALL_SIZE_SET) {
            final long size = getSize(type);
            if (UNKNOWN != size)
                f.format(", size(%s)=%d", type, size);
        }
        for (Access type : ALL_ACCESS_SET) {
            final long time = getTime(type);
            if (UNKNOWN != time)
                f.format(", time(%s)=%tc", type, time);
        }
        return f.format(",members=%s]", getMembers()).toString();
    }
}
