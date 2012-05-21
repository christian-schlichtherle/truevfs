/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.truevfs.driver.zip.it;

import net.truevfs.access.ConcurrentSyncITSuite;
import net.truevfs.driver.zip.ZipDriver;
import net.truevfs.kernel.cio.IOPool;

/**
 * @author  Christian Schlichtherle
 */
public final class ZipConcurrentSyncIT extends ConcurrentSyncITSuite<ZipDriver> {

    @Override
    protected String getExtensionList() {
        return "zip";
    }

    @Override
    protected ZipDriver newArchiveDriver() {
        return new ZipDriver() {
            @Override
            public IOPool<?> getIoPool() {
                return getTestConfig().getIoPoolProvider().getIoPool();
            }
        };
    }
}