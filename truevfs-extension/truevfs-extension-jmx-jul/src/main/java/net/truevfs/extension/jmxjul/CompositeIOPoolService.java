/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.truevfs.extension.jmxjul;

import javax.annotation.concurrent.Immutable;
import net.truevfs.driver.file.TempFilePoolService;
import net.truevfs.extension.jmxjul.jmx.JmxDirector;
import net.truevfs.extension.jmxjul.jul.JulDirector;
import net.truevfs.kernel.cio.IOPool;
import net.truevfs.kernel.spi.IOPoolService;

/**
 * @author Christian Schlichtherle
 */
@Immutable
public final class CompositeIOPoolService extends IOPoolService {

    private final IOPoolService service = new TempFilePoolService();

    @SuppressWarnings("unchecked")
    private final IOPool<?> pool =
            JmxDirector.SINGLETON.instrument(
                JulDirector.SINGLETON.instrument(
                    (IOPool) service.getIoPool()));

    @Override
    public IOPool<?> getIoPool() {
        return pool;
    }

    /** @return 100 */
    @Override
    public int getPriority() {
        return 100;
    }
}
