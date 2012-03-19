/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.fs.inst.comp;

import de.schlichtherle.truezip.fs.FsDefaultManager;
import de.schlichtherle.truezip.fs.FsFailSafeManager;
import de.schlichtherle.truezip.fs.FsManager;
import de.schlichtherle.truezip.fs.inst.jmx.JmxDirector;
import de.schlichtherle.truezip.fs.inst.jul.JulDirector;
import de.schlichtherle.truezip.fs.spi.FsManagerService;
import javax.annotation.concurrent.Immutable;

/**
 * @author  Christian Schlichtherle
 */
@Immutable
public final class CompositeManagerService extends FsManagerService {

    private static final FsManager manager =
            JmxDirector.SINGLETON.instrument(
                JulDirector.SINGLETON.instrument(
                    new FsFailSafeManager(
                        new FsDefaultManager())));

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