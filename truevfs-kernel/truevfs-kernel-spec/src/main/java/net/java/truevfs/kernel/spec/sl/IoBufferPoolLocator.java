/*
 * Copyright (C) 2005-2013 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.kernel.spec.sl;

import javax.annotation.concurrent.Immutable;
import net.java.truecommons.cio.IoBufferPool;
import net.java.truecommons.services.Container;
import net.java.truecommons.services.ServiceLocator;
import net.java.truevfs.kernel.spec.spi.IoBufferPoolDecorator;
import net.java.truevfs.kernel.spec.spi.IoBufferPoolFactory;

/**
 * A container of the singleton I/O buffer pool.
 * The I/O buffer pool is created by using a {@link ServiceLocator} to search for
 * advertised implementations of the factory service specification class
 * {@link IoBufferPoolFactory}
 * and the decorator service specification class
 * {@link IoBufferPoolDecorator}.
 *
 * @author Christian Schlichtherle
 */
@Immutable
public final class IoBufferPoolLocator implements Container<IoBufferPool> {

    /** The singleton instance of this class. */
    public static final IoBufferPoolLocator SINGLETON = new IoBufferPoolLocator();

    private IoBufferPoolLocator() { }

    @Override
    public IoBufferPool get() { return Lazy.pool; }

    /** A static data utility class used for lazy initialization. */
    private static final class Lazy {
        static final IoBufferPool pool
                = new ServiceLocator(Lazy.class)
                .factory(IoBufferPoolFactory.class, IoBufferPoolDecorator.class)
                .get();
    }
}
