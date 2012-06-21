/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.truevfs.driver.tar.it;

import net.truevfs.access.ConcurrentSyncITSuite;
import net.truevfs.driver.tar.TarGZipDriver;
import net.truevfs.kernel.spec.cio.IoPool;

/**
 * @author Christian Schlichtherle
 */
public final class TarGZipConcurrentSyncIT
extends ConcurrentSyncITSuite<TarGZipDriver> {

    @Override
    protected String getExtensionList() {
        return "tar.gz";
    }

    @Override
    protected TarGZipDriver newArchiveDriver() {
        return new TarGZipDriver() {
            @Override
            public IoPool<?> getIoPool() {
                return getTestConfig().getIoPoolProvider().getIoPool();
            }
        };
    }
}
