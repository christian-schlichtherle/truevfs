/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.truevfs.ext.comp;

import javax.annotation.concurrent.Immutable;
import net.truevfs.driver.file.TempFilePoolService;
import net.truevfs.ext.jmx.JmxDirector;
import net.truevfs.ext.jul.JulDirector;
import net.truevfs.kernel.spec.cio.IoBuffer;
import net.truevfs.kernel.spec.cio.IoPool;
import net.truevfs.kernel.spec.spi.IoPoolService;

/**
 * @author Christian Schlichtherle
 */
@Immutable
public final class CompositeIoPoolService extends IoPoolService {

    private final IoPoolService service = new TempFilePoolService();

    @SuppressWarnings("unchecked")
    private final IoPool<?> pool =
            JmxDirector.SINGLETON.instrument(
                JulDirector.SINGLETON.instrument(
                    (IoPool) service.getIoPool()));

    @Override
    public IoPool<? extends IoBuffer<?>> getIoPool() {
        return pool;
    }

    /** @return 100 */
    @Override
    public int getPriority() {
        return 100;
    }
}
