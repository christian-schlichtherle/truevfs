/*
 * Copyright © 2005 - 2021 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package global.namespace.truevfs.driver.mock;

import global.namespace.truevfs.comp.cio.Entry;
import global.namespace.truevfs.comp.cio.IoBuffer;
import global.namespace.truevfs.comp.cio.IoBufferPool;
import global.namespace.truevfs.kernel.api.FsArchiveEntries;
import global.namespace.truevfs.kernel.api.FsArchiveEntry;

import javax.annotation.CheckForNull;
import java.io.IOException;
import java.util.EnumMap;
import java.util.Objects;
import java.util.Optional;

/**
 * @author Christian Schlichtherle
 */
public final class MockArchiveDriverEntry implements FsArchiveEntry {

    private final String name;
    private final Type type;
    private final EnumMap<Size, Long> sizes = new EnumMap<>(Size.class);
    private final EnumMap<Access, Long> times = new EnumMap<>(Access.class);
    private final Boolean[][] permissions = new Boolean[Access.values().length][PosixEntity.values().length];
    private @CheckForNull
    IoBuffer buffer;

    public MockArchiveDriverEntry(String name, Type type) {
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
                if (UNKNOWN != value) {
                    sizes.put(size, value);
                }
            }
            for (final Access access : ALL_ACCESS) {
                final long value = template.getTime(access);
                if (UNKNOWN != value) {
                    times.put(access, value);
                }
                for (PosixEntity entity : ALL_POSIX_ENTITIES) {
                    permissions[access.ordinal()][entity.ordinal()] =
                            template.isPermitted(access, entity).orElse(null);
                }
            }
        }
    }

    IoBuffer getBuffer(final IoBufferPool pool) throws IOException {
        final IoBuffer buffer = this.buffer;
        return null != buffer ? buffer : (this.buffer = pool.allocate());
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
    public Optional<Boolean> isPermitted(Access type, Entity entity) {
        return entity instanceof PosixEntity
                ? Optional.ofNullable(permissions[type.ordinal()][((PosixEntity) entity).ordinal()])
                : Optional.empty();
    }

    @Override
    public boolean setPermitted(Access type, Entity entity, Optional<Boolean> value) {
        if (entity instanceof PosixEntity) {
            permissions[type.ordinal()][((PosixEntity) entity).ordinal()] = value.orElse(null);
            return true;
        } else {
            return false;
        }
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
