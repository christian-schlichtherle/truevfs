/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.truevfs.driver.tar.it;

import net.truevfs.access.ConcurrentSyncITSuite;
import net.truevfs.driver.tar.TarBZip2Driver;
import net.truevfs.kernel.spec.cio.IoPool;

/**
 * @author Christian Schlichtherle
 */
public final class TarBZip2ConcurrentSyncIT
extends ConcurrentSyncITSuite<TarBZip2Driver> {

    @Override
    protected String getExtensionList() {
        return "tar.bz2";
    }

    @Override
    protected TarBZip2Driver newArchiveDriver() {
        return new TarBZip2Driver() {
            @Override
            public IoPool<?> getIoPool() {
                return getTestConfig().getIoPoolProvider().getIoPool();
            }
        };
    }
}
