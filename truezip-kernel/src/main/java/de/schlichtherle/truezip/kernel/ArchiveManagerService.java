/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.kernel;

import de.truezip.kernel.FsManager;
import de.truezip.kernel.spi.FsManagerService;
import java.util.logging.Logger;
import javax.annotation.concurrent.Immutable;

/**
 * A service for the file system manager implementation in this package.
 * 
 * @author Christian Schlichtherle
 */
@Immutable
public final class ArchiveManagerService extends FsManagerService {

    static {
        Logger  .getLogger( ArchiveManagerService.class.getName(),
                            ArchiveManagerService.class.getName())
                .config("banner");
    }

    private final FsManager manager = new ArchiveManager();

    @Override
    public FsManager getManager() {
        return manager;
    }

    /** @return -100 */
    @Override
    public int getPriority() {
        return -100;
    }
}
