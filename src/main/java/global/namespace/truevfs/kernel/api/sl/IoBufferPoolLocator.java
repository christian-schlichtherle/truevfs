/*
 * Copyright © 2005 - 2021 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package global.namespace.truevfs.kernel.api.sl;

import global.namespace.service.wight.core.ServiceLocator;
import global.namespace.truevfs.commons.cio.IoBufferPool;
import global.namespace.truevfs.kernel.api.spi.IoBufferPoolDecorator;
import global.namespace.truevfs.kernel.api.spi.IoBufferPoolFactory;

import java.util.function.Supplier;

/**
 * A supplier of the singleton I/O buffer pool.
 * The I/O buffer pool is created by using a {@link ServiceLocator} to search for published implementations of the
 * factory service interface {@link IoBufferPoolFactory} and the decorator service interface
 * {@link IoBufferPoolDecorator}.
 *
 * @author Christian Schlichtherle
 */
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
