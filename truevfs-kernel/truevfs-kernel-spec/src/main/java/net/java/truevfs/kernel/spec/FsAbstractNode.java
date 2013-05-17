/*
 * Copyright (C) 2005-2013 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.kernel.spec;

import java.util.Formatter;
import net.java.truecommons.shed.UniqueObject;

/**
 * An abstract file system node is a node which can implement multiple types
 * and list directory members.
 *
 * @since  TrueVFS 0.10
 * @author Christian Schlichtherle
 */
public abstract class FsAbstractNode
extends UniqueObject implements FsNode {

    @Override
    public boolean isType(Type type) { return getTypes().is(type); }

    /**
     * Returns a string representation of this object for logging and debugging
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
