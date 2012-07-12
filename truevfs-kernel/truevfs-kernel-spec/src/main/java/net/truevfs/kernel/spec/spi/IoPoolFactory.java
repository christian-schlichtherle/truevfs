/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.truevfs.kernel.spec.spi;

import net.truevfs.kernel.spec.cio.IoBuffer;
import net.truevfs.kernel.spec.cio.IoPool;
import net.truevfs.kernel.spec.cio.IoPoolProvider;
import net.truevfs.kernel.spec.sl.IoPoolLocator;

/**
 * An abstract locatable service for creating I/O buffer pools.
 * Implementations of this abstract class are subject to service location
 * by the class {@link IoPoolLocator}.
 *
 * @author Christian Schlichtherle
 */
public abstract class IoPoolFactory
extends ServiceProvider
implements IoPoolProvider {

    /**
     * Returns a new pool to use for allocating temporary I/O buffers.
     *
     * @return A new pool to use for allocating temporary I/O buffers.
     */
    @Override
    public abstract IoPool<? extends IoBuffer<?>> ioPool();
}
