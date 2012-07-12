/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.truevfs.kernel.spec.spi;

import net.truevfs.kernel.spec.FsManager;
import net.truevfs.kernel.spec.FsManagerProvider;
import net.truevfs.kernel.spec.sl.FsManagerLocator;

/**
 * An abstract locatable service for creating file system managers.
 * Implementations of this abstract class are subject to service location
 * by the class {@link FsManagerLocator}.
 *
 * @author Christian Schlichtherle
 */
public abstract class FsManagerFactory
extends ServiceProvider
implements FsManagerProvider {

    /**
     * Returns a new file system manager.
     *
     * @return A new file system manager.
     */
    @Override
    public abstract FsManager manager();
}
