/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.truezip.extension.jmxjul;

import de.truezip.driver.file.TempFilePoolService;
import de.truezip.extension.jmxjul.jmx.JmxDirector;
import de.truezip.extension.jmxjul.jul.JulDirector;
import de.truezip.kernel.cio.IOPool;
import de.truezip.kernel.spi.IOPoolService;
import javax.annotation.concurrent.Immutable;

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
                    (IOPool) service.getIOPool()));

    @Override
    public IOPool<?> getIOPool() {
        return pool;
    }

    /** @return 100 */
    @Override
    public int getPriority() {
        return 100;
    }
}
