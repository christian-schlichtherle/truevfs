/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truevfs.kernel.se;

import net.truevfs.kernel.FsManager;
import net.truevfs.kernel.spi.FsManagerService;
import javax.annotation.concurrent.Immutable;

/**
 * A service for the file system manager implementation in this package.
 * 
 * @author Christian Schlichtherle
 */
@Immutable
public final class ArchiveManagerService extends FsManagerService {

    @Override
    public FsManager getManager() {
        return Boot.manager;
    }

    @Override
    public int getPriority() {
        return -100;
    }

    private static final class Boot {
        static final FsManager manager = new ArchiveManager();
    }
}
