/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.kernel.spec;

import java.util.Formatter;
import java.util.Set;
import javax.annotation.Nullable;
import net.java.truecommons.shed.BitField;
import net.java.truecommons.shed.UniqueObject;
import net.java.truecommons.cio.Entry;

/**
 * An abstract file system node is a node which can implement multiple types
 * and list directory members.
 *
 * @author Christian Schlichtherle
 */
public abstract class FsNode
extends UniqueObject implements Entry {

    /**
     * Returns a string representation of the
     * {@link FsNodeName file system node name}.
     *
     * @return A string representation of the
     *         {@link FsNodeName file system node name}.
     */
    @Override
    public abstract String getName();

    /**
     * Returns a bit field of types implemented by this file system node.
     * <p>
     * Some file system types allow a node to implement multiple types.
     * For example, a ZIP or TAR file may contain an archive entry with the
     * name {@code foo} and a directory archive entry with the name {@code foo/}.
     * Yes, this is strange, but sh*t happens!
     * In this case then, a virtual file system should collapse this into one
     * file system node which returns {@code true} for both
     * {@code isType(FILE)} and {@code isType(DIRECTORY)}.
     * 
     * @return A bit field of types implemented by this file system node.
     */
    public abstract BitField<Type> getTypes();

    /**
     * Returns {@code true} if and only if this file system node implements
     * the given type.
     *
     * @param  type the type to test.
     * @return {@code true} if and only if this file system node implements
     *         the given type.
     * @see    #getTypes()
     */
    public boolean isType(Type type) { return getTypes().is(type); }

    /**
     * Returns a set of strings with the base names of the members of this
     * directory node or {@code null} if and only if this is not a directory
     * node.
     * Whether or not modifying the returned set is supported and the effect
     * on the file system is implementation specific.
     * 
     * @return A set of strings with the base names of the members of this
     *         directory node or {@code null} if and only if this is not a
     *         directory node.
     */
    public abstract @Nullable Set<String> getMembers();

    /**
     * Returns a string representation of this object for debugging and logging
     * purposes.
     */
    @Override
    public String toString() {
        final StringBuilder s = new StringBuilder(256);
        final Formatter f = new Formatter(s).format("%s@%x[name=%s, types=%s",
                getClass().getName(), hashCode(), getName(), getTypes());
        for (final Size type : ALL_SIZES) {
            final long size = getSize(type);
            if (UNKNOWN != size) f.format(", size(%s)=%d", type, size);
        }
        for (final Access type : ALL_ACCESS) {
            final long time = getTime(type);
            if (UNKNOWN != time) f.format(", time(%s)=%tc", type, time);
        }
        return f.format(", members=%s]", getMembers()).toString();
    }
}