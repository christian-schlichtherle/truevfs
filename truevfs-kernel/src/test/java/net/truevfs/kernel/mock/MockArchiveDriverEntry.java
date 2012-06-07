/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.truevfs.kernel.mock;

import java.io.IOException;
import java.util.EnumMap;
import java.util.Objects;
import javax.annotation.CheckForNull;
import net.truevfs.kernel.FsArchiveEntries;
import net.truevfs.kernel.FsArchiveEntry;
import net.truevfs.kernel.cio.Entry;
import net.truevfs.kernel.cio.Entry.Access;
import net.truevfs.kernel.cio.Entry.Entity;
import net.truevfs.kernel.cio.Entry.PosixEntity;
import net.truevfs.kernel.cio.Entry.Size;
import net.truevfs.kernel.cio.Entry.Type;
import net.truevfs.kernel.cio.IoBuffer;
import net.truevfs.kernel.cio.IoPool;

/**
 * @author Christian Schlichtherle
 */
public final class MockArchiveDriverEntry implements FsArchiveEntry {

    private final String name;
    private final Type type;
    private final EnumMap<Size, Long>
            sizes = new EnumMap<>(Size.class);
    private final EnumMap<Access, Long>
            times = new EnumMap<>(Access.class);
    private final Boolean[][] permissions
            = new Boolean[Access.values().length][PosixEntity.values().length];
    private @CheckForNull IoBuffer<?> buffer;

    public MockArchiveDriverEntry(final String name, final Type type) {
        this(name, type, null);
    }

    public MockArchiveDriverEntry(
            final String name,
            final Type type,
            final @CheckForNull Entry template) {
        this.name = Objects.requireNonNull(name);
        this.type = Objects.requireNonNull(type);
        if (null != template) {
            for (final Size size : ALL_SIZES) {
                final long value = template.getSize(size);
                if (UNKNOWN != value)
                    sizes.put(size, value);
            }
            for (final Access access : ALL_ACCESS) {
                final long value = template.getTime(access);
                if (UNKNOWN != value)
                    times.put(access, value);
                for (final PosixEntity entity : ALL_POSIX_ENTITIES)
                    permissions[access.ordinal()][entity.ordinal()]
                            = template.isPermitted(access, entity);
            }
        }
    }

    IoBuffer<?> getBuffer(final IoPool<?> ioPool) throws IOException {
        final IoBuffer<?> buffer = this.buffer;
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

    @Override
    public Boolean isPermitted(Access type, Entity entity) {
        if (!(entity instanceof PosixEntity))
            return null;
        return permissions[type.ordinal()][((PosixEntity) entity).ordinal()];
    }

    @Override
    public boolean setPermitted(Access type, Entity entity, Boolean value) {
        if (!(entity instanceof PosixEntity))
            return false;
        permissions[type.ordinal()][((PosixEntity) entity).ordinal()] = value;
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