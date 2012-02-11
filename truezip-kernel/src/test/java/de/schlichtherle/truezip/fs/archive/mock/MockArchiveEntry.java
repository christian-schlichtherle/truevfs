/*
 * Copyright 2004-2012 Schlichtherle IT Services
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package de.schlichtherle.truezip.fs.archive.mock;

import de.schlichtherle.truezip.entry.Entry;
import de.schlichtherle.truezip.fs.archive.FsArchiveEntry;
import de.schlichtherle.truezip.socket.IOPool;
import java.util.EnumMap;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

/**
 * @author  Christian Schlichtherle
 * @version $Id$
 */
public final class MockArchiveEntry implements FsArchiveEntry {

    private final String name;
    private final Type type;
    private final EnumMap<Size, Long>
            sizes = new EnumMap<Size, Long>(Size.class);
    private final EnumMap<Access, Long>
            times = new EnumMap<Access, Long>(Access.class);
    @Nullable IOPool.Entry<?> io;

    public MockArchiveEntry(final String name,
                            final Type type,
                            final @CheckForNull Entry template) {
        assert null != name;
        assert null != type;
        this.name = name;
        this.type = type;
        if (null != template) {
            for (final Size size : ALL_SIZE_SET) {
                final long value = template.getSize(size);
                if (UNKNOWN != value)
                    sizes.put(size, value);
            }
            for (final Access access : ALL_ACCESS_SET) {
                final long value = template.getTime(access);
                if (UNKNOWN != value)
                    times.put(access, value);
            }
        }
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Type getType() {
        return type;
    }

    @Override
    public long getSize(final Size type) {
        final Long size = sizes.get(type);
        return null != size ? size : UNKNOWN;
    }

    @Override
    public boolean setSize(final Size type, final long value) {
        sizes.put(type, value);
        return true;
    }

    @Override
    public long getTime(final Access type) {
        final Long time = times.get(type);
        return null != time ? time : UNKNOWN;
    }

    @Override
    public boolean setTime(final Access type, final long value) {
        times.put(type, value);
        return true;
    }

    /**
     * Returns a string representation of this object for debugging and logging
     * purposes.
     */
    @Override
    public String toString() {
        final StringBuilder s = new StringBuilder(getClass().getName())
                .append("[name=").append(getName())
                .append(",type=").append(getType());
        for (Size type : ALL_SIZE_SET)
            s.append(",size(").append(type).append(")=").append(getSize(type));
        for (Access type : ALL_ACCESS_SET)
            s.append(",time(").append(type).append(")=").append(getTime(type));
        return s.append("]").toString();
    }
}
