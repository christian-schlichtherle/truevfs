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
public interface IoPoolProvider {

    /**
     * Returns a pool for allocating temporary I/O buffers.
     *
     * @return A pool for allocating temporary I/O buffers.
     */
    IoPool<? extends IoBuffer<?>> ioPool();
}
