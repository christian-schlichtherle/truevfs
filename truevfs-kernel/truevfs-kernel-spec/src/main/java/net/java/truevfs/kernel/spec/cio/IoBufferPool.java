/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.kernel.spec.cio;

import java.io.IOException;
import net.java.truecommons.shed.Pool;

/**
 * A pool for allocating I/O buffers, which can get used as a volatile storage
 * for bulk I/O.
 * Typical implementations may use temporary files for big data or byte arrays
 * for small data.
 * <p>
 * Implementations must be thread-safe.
 * However, this does not necessarily apply to its managed I/O buffers.
 *
 * @param  <B> the type of the I/O buffers managed by this pool.
 * @author Christian Schlichtherle
 */
@SuppressWarnings("MarkerInterface")
public interface IoBufferPool<B extends IoBuffer<B>>
extends Pool<IoBuffer<B>, IOException> {
}
