/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.entry;

import de.schlichtherle.truezip.util.Pool;
import java.io.IOException;

/**
 * A pool for allocating I/O buffers, which are used as a volatile storage for
 * bulk data.
 * Typical implementations may use temporary files for big data or byte arrays
 * for small data.
 * The I/O buffers are referred to by {@link IOEntry}s.
 * <p>
 * Implementations must be thread-safe.
 * However, this does not necessarily apply to the implementation of its
 * managed resources, i.e. the {@code IOEntry}s.
 *
 * @param  <E> the type of the I/O entries in this pool.
 * @author Christian Schlichtherle
 */
public interface IOPool<E extends IOEntry<E>>
extends Pool<IOPool.IOBuffer<E>, IOException> {

    /**
     * A releasable I/O buffer.
     * 
     * @param <E> the type of the I/O entries.
     */
    @SuppressWarnings("PublicInnerClass")
    interface IOBuffer<E extends IOEntry<E>>
    extends IOEntry<E>, Pool.Releasable<IOException> {
    }
}
