/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.kernel.fs;

import de.truezip.kernel.fs.FsManager;
import de.truezip.kernel.fs.spi.FsManagerService;
import javax.annotation.concurrent.Immutable;

/**
 * @author Christian Schlichtherle
 */
@Immutable
public final class FsFailSafeManagerService extends FsManagerService {

    private final FsManager
            manager = new FsFailSafeManager(new FsArchiveManager());

    @Override
    public FsManager get() {
        return manager;
    }
}