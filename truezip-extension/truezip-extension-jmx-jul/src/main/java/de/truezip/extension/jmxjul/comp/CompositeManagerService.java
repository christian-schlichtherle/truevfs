/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.truezip.extension.jmxjul.comp;

import de.schlichtherle.truezip.kernel.fs.FsFailSafeManagerService;
import de.truezip.extension.jmxjul.jmx.JmxDirector;
import de.truezip.extension.jmxjul.jul.JulDirector;
import de.truezip.kernel.fs.FsManager;
import de.truezip.kernel.fs.spi.FsManagerService;
import javax.annotation.concurrent.Immutable;

/**
 * @author  Christian Schlichtherle
 */
@Immutable
public final class CompositeManagerService extends FsManagerService {

    private final FsManager manager =
            JmxDirector.SINGLETON.instrument(
                JulDirector.SINGLETON.instrument(
                    new FsFailSafeManagerService().get()));

    @Override
    public FsManager get() {
        return manager;
    }

    /**
     * Returns 100.
     * 
     * @return 100.
     */
    @Override
    public int getPriority() {
        return 100;
    }
}