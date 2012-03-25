/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.truezip.extension.jmxjul.comp;

import de.truezip.extension.jmxjul.jmx.JmxDirector;
import de.truezip.extension.jmxjul.jul.JulDirector;
import de.schlichtherle.truezip.cio.IOPool;
import de.schlichtherle.truezip.cio.spi.IOPoolService;
import javax.annotation.concurrent.Immutable;

/**
 * @author Christian Schlichtherle
 */
@Immutable
public final class CompositeIOPoolService extends IOPoolService {

    private final IOPoolService service;
    {
        final IOPoolService
                oio = new de.truezip.driver.file.oio.TempFilePoolService();
        final IOPoolService
                nio = new de.truezip.driver.file.nio.TempFilePoolService();
        service = oio.getPriority() > nio.getPriority() ? oio : nio;
    }

    @SuppressWarnings("unchecked")
    private final IOPool<?> pool =
            JmxDirector.SINGLETON.instrument(
                JulDirector.SINGLETON.instrument(
                    (IOPool) service.get()));

    @Override
    public IOPool<?> get() {
        return pool;
    }

    /**
     * Returns 1 iff the JVM is running JSE 6 or 151 iff the JVM is running
     * JSE 7.
     * 
     * @return 1 iff the JVM is running JSE 6 or 151 iff the JVM is running
     *         JSE 7.
     */
    @Override
    public int getPriority() {
        return service.getPriority() * 3 / 2 + 1;
    }
}
