/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.truevfs.extension.jmxjul;

import javax.annotation.concurrent.Immutable;
import net.truevfs.driver.file.TempFilePoolService;
import net.truevfs.extension.jmxjul.jmx.JmxDirector;
import net.truevfs.extension.jmxjul.jul.JulDirector;
import net.truevfs.kernel.cio.IoPool;
import net.truevfs.kernel.spi.IoPoolService;

/**
 * @author Christian Schlichtherle
 */
@Immutable
public final class CompositeIOPoolService extends IoPoolService {

    private final IoPoolService service = new TempFilePoolService();

    @SuppressWarnings("unchecked")
    private final IoPool<?> pool =
            JmxDirector.SINGLETON.instrument(
                JulDirector.SINGLETON.instrument(
                    (IoPool) service.getIoPool()));

    @Override
    public IoPool<?> getIoPool() {
        return pool;
    }

    /** @return 100 */
    @Override
    public int getPriority() {
        return 100;
    }
}
