/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.extension.throttle;

import de.schlichtherle.truezip.fs.FsDefaultManager;
import de.schlichtherle.truezip.fs.FsManager;
import de.schlichtherle.truezip.fs.spi.FsManagerService;
import javax.annotation.concurrent.Immutable;

/**
 * @author Christian Schlichtherle
 */
@Immutable
public final class ThrottleManagerService extends FsManagerService {

    private static final FsManager manager =
            new ThrottleManager(new FsDefaultManager());

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
