/*
 * Copyright (C) 2005-2015 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truecommons.cio;

import java.io.IOException;
import java.util.Objects;
import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;

/**
 * An abstract decorator for an I/O buffer pool.
 *
 * @author Christian Schlichtherle
 */
@ThreadSafe
public abstract class DecoratingIoBufferPool extends IoBufferPool {
    protected @Nullable IoBufferPool pool;

    protected DecoratingIoBufferPool() { }

    protected DecoratingIoBufferPool(final IoBufferPool pool) {
        this.pool = Objects.requireNonNull(pool);
    }

    @Override
    public IoBuffer allocate() throws IOException {
        return pool.allocate();
    }

    /**
     * Returns a string representation of this object for debugging and logging
     * purposes.
     */
    @Override
    public String toString() {
        return String.format("%s@%x[pool=%s]",
                getClass().getName(), hashCode(), pool);
    }
}
