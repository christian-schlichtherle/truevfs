/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.truevfs.kernel.spec.spi;

import javax.annotation.concurrent.ThreadSafe;
import net.truevfs.kernel.spec.cio.IoBuffer;
import net.truevfs.kernel.spec.cio.IoBufferPool;
import net.truevfs.kernel.spec.sl.IoBufferPoolLocator;

/**
 * An abstract locatable service for decorating I/O buffer pools.
 * Implementations of this abstract class are subject to service location
 * by the class {@link IoBufferPoolLocator}.
 * <p>
 * Implementations should be thread-safe.
 * 
 * @author Christian Schlichtherle
 */
@ThreadSafe
public abstract class IoBufferPoolDecorator extends ServiceProvider {

    /**
     * Decorates the given I/O buffer pool.
     * 
     * @param  <B> the type of the I/O buffers managed by the pool.
     * @param  pool the I/O buffer pool to decorate.
     * @return The decorated I/O buffer pool.
     */
    public abstract <B extends IoBuffer<B>> IoBufferPool<B> decorate(IoBufferPool<B> pool);

    /**
     * Returns a priority to help service locators to prioritize the services
     * provided by this object.
     * The decorators will be applied in ascending order of priority so that
     * the decorator with the greatest number becomes the head of the decorator
     * chain.
     * <p>
     * The implementation in the class {@link IoBufferPoolDecorator} returns
     * zero.
     * 
     * @return A priority to help service locators to prioritize the services
     *         provided by this object.
     */
    @Override
    public int getPriority() {
        return 0;
    }
}
