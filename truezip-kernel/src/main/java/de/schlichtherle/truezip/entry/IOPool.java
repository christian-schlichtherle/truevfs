/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.entry;

import de.schlichtherle.truezip.util.Pool;
import java.io.IOException;

/**
 * A pool for allocating I/O buffers, which can get used as a volatile storage
 * for bulk I/O.
 * Typical implementations may use temporary files for big data or byte arrays
 * for small data.
 * <p>
 * Implementations must be thread-safe.
 * However, this does not necessarily apply to its managed I/O buffers.
 *
 * @param  <B> the type parameter for the I/O buffers managed by this pool.
 * @author Christian Schlichtherle
 */
@SuppressWarnings("MarkerInterface")
public interface IOPool<B extends IOBuffer<B>>
extends Pool<IOBuffer<B>, IOException> {
}