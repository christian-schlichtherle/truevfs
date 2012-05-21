/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.truevfs.driver.tar.it;

import net.truevfs.access.ConcurrentSyncITSuite;
import net.truevfs.driver.tar.TarDriver;
import net.truevfs.kernel.cio.IOPool;

/**
 * @author Christian Schlichtherle
 */
public final class TarConcurrentSyncIT
extends ConcurrentSyncITSuite<TarDriver> {

    @Override
    protected String getExtensionList() {
        return "tar";
    }

    @Override
    protected TarDriver newArchiveDriver() {
        return new TarDriver() {
            @Override
            public IOPool<?> getIoPool() {
                return getTestConfig().getIoPoolProvider().getIoPool();
            }
        };
    }
}
