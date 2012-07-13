/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.truevfs.ext.comp;

import javax.annotation.concurrent.Immutable;
import net.truevfs.driver.file.TempFilePoolFactory;
import net.truevfs.ext.jmx.JmxDirector;
import net.truevfs.ext.jul.JulDirector;
import net.truevfs.kernel.spec.cio.IoBuffer;
import net.truevfs.kernel.spec.cio.IoBufferPool;
import net.truevfs.kernel.spec.spi.IoBufferPoolFactory;

/**
 * @author Christian Schlichtherle
 */
@Immutable
public final class CompositeIoBufferPoolFactory extends IoBufferPoolFactory {

    private final IoBufferPoolFactory service = new TempFilePoolFactory();

    private final IoBufferPool<?> pool =
            JmxDirector.SINGLETON.instrument(
                JulDirector.SINGLETON.instrument(
                    (IoBufferPool<?>) service.ioBufferPool()));

    @Override
    public IoBufferPool<? extends IoBuffer<?>> ioBufferPool() {
        return pool;
    }

    /** @return 100 */
    @Override
    public int getPriority() {
        return 100;
    }
}
