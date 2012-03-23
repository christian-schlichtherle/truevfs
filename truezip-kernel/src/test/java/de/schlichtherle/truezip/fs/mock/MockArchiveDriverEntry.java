/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.fs.mock;

import de.schlichtherle.truezip.entry.Entry;
import de.schlichtherle.truezip.fs.FsArchiveEntries;
import de.schlichtherle.truezip.fs.FsArchiveEntry;
import de.schlichtherle.truezip.socket.IOPool;
import java.io.IOException;
import java.util.EnumMap;
import javax.annotation.CheckForNull;

/**
 * @author  Christian Schlichtherle
 */
public final class MockArchiveDriverEntry implements FsArchiveEntry {

    private final String name;
    private final Type type;
    private final EnumMap<Size, Long>
            sizes = new EnumMap<Size, Long>(Size.class);
    private final EnumMap<Access, Long>
            times = new EnumMap<Access, Long>(Access.class);
    private @CheckForNull IOPool.Buffer<?> buffer;

    public MockArchiveDriverEntry(  final String name,
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

    IOPool.Buffer<?> getBuffer(final IOPool<?> ioPool) throws IOException {
        final IOPool.Buffer<?> buffer = this.buffer;
        return null != buffer ? buffer : (this.buffer = ioPool.allocate());
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
        return FsArchiveEntries.toString(this);
    }
}