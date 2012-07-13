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
import net.truevfs.kernel.spec.spi.IoPoolFactory;

/**
 * @author Christian Schlichtherle
 */
@Immutable
public final class CompositeIoPoolFactory extends IoPoolFactory {

    private final IoPoolFactory service = new TempFilePoolFactory();

    private final IoBufferPool<?> pool =
            JmxDirector.SINGLETON.instrument(
                JulDirector.SINGLETON.instrument(
                    (IoBufferPool<?>) service.ioPool()));

    @Override
    public IoBufferPool<? extends IoBuffer<?>> ioPool() {
        return pool;
    }

    /** @return 100 */
    @Override
    public int getPriority() {
        return 100;
    }
}
