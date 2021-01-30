/*
 * Copyright © 2005 - 2021 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.kernel.spec.sl;

import global.namespace.service.wight.core.ServiceLocator;
import net.java.truecommons.cio.IoBufferPool;
import net.java.truevfs.kernel.spec.spi.IoBufferPoolDecorator;
import net.java.truevfs.kernel.spec.spi.IoBufferPoolFactory;

import javax.annotation.concurrent.Immutable;
import java.util.function.Supplier;

/**
 * A supplier of the singleton I/O buffer pool.
 * The I/O buffer pool is created by using a {@link ServiceLocator} to search for published implementations of the
 * factory service interface {@link IoBufferPoolFactory} and the decorator service interface
 * {@link IoBufferPoolDecorator}.
 *
 * @author Christian Schlichtherle
 */
@Immutable
public final class IoBufferPoolLocator implements Supplier<IoBufferPool> {

    /**
     * The singleton instance of this class.
     */
    public static final IoBufferPoolLocator SINGLETON = new IoBufferPoolLocator();

    private IoBufferPoolLocator() {
    }

    @Override
    public IoBufferPool get() {
        return Lazy.pool;
    }

    private static final class Lazy {
        static final IoBufferPool pool =
                new ServiceLocator().provider(IoBufferPoolFactory.class, IoBufferPoolDecorator.class).get();
    }
}
