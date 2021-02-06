/*
 * Copyright (C) 2005-2015 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package global.namespace.truevfs.comp.cio;

import java.util.Objects;
import java.util.Optional;

/**
 * An abstract decorator for an entry.
 *
 * @param <E> the type of the decorated entry.
 * @author Christian Schlichtherle
 */
public abstract class DecoratingEntry<E extends Entry> implements Entry {

    /**
     * The decorated entry.
     */
    protected E entry;

    protected DecoratingEntry() {
    }

    protected DecoratingEntry(final E entry) {
        this.entry = Objects.requireNonNull(entry);
    }

    @Override
    public String getName() {
        return entry.getName();
    }

    @Override
    public long getSize(Size type) {
        return entry.getSize(type);
    }

    @Override
    public long getTime(Access type) {
        return entry.getTime(type);
    }

    @Override
    public Optional<Boolean> isPermitted(Access type, Entity entity) {
        return entry.isPermitted(type, entity);
    }
}
