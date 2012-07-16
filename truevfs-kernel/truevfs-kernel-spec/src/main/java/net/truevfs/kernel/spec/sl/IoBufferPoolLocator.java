/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.truevfs.kernel.spec.sl;

import de.schlichtherle.truecommons.services.Container;
import de.schlichtherle.truecommons.services.Locator;
import javax.annotation.concurrent.Immutable;
import net.truevfs.kernel.spec.cio.IoBuffer;
import net.truevfs.kernel.spec.cio.IoBufferPool;
import net.truevfs.kernel.spec.spi.IoBufferPoolDecorator;
import net.truevfs.kernel.spec.spi.IoBufferPoolFactory;

/**
 * Uses a {@link Locator} to resolve the singleton I/O buffer pool from
 * instances of the factory service class {@link IoBufferPoolFactory} and the
 * decorator service class {@link IoBufferPoolDecorator}.
 *
 * @author Christian Schlichtherle
 */
@Immutable
public final class IoBufferPoolLocator
implements Container<IoBufferPool<? extends IoBuffer<?>>> {

    /** The singleton instance of this class. */
    public static final IoBufferPoolLocator SINGLETON = new IoBufferPoolLocator();

    private IoBufferPoolLocator() { }

    @Override
    public IoBufferPool<? extends IoBuffer<?>> apply() { return Boot.pool; }

    /** A static data utility class used for lazy initialization. */
    private static final class Boot {
        static final IoBufferPool<? extends IoBuffer<?>> pool = new Locator(IoBufferPoolLocator.class)
                .factory(IoBufferPoolFactory.class, IoBufferPoolDecorator.class)
                .apply();
    }
}
