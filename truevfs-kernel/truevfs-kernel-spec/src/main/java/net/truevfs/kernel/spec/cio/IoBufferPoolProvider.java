/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.truevfs.kernel.spec.cio;

import javax.annotation.concurrent.ThreadSafe;

/**
 * Provides I/O buffer pools.
 * <p>
 * Implementations must be thread-safe.
 *
 * @author Christian Schlichtherle
 */
@ThreadSafe
public interface IoBufferPoolProvider {

    /**
     * Returns a pool for allocating temporary I/O buffers.
     * <p>
     * Implementations are free to return the same instance (property method)
     * or a new instance (factory method) upon each call.
     * So clients may need to cache the result for future reuse.
     *
     * @return A pool for allocating temporary I/O buffers.
     */
    IoBufferPool<? extends IoBuffer<?>> pool();
}
