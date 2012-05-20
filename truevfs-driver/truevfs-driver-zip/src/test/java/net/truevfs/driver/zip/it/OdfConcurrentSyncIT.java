/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.truevfs.driver.zip.it;

import net.truevfs.driver.zip.OdfDriver;
import net.truevfs.access.ConcurrentSyncITSuite;
import net.truevfs.kernel.cio.IOPool;

/**
 * @author Christian Schlichtherle
 */
public final class OdfConcurrentSyncIT extends ConcurrentSyncITSuite<OdfDriver> {

    @Override
    protected String getExtensionList() {
        return "odf";
    }

    @Override
    protected OdfDriver newArchiveDriver() {
        return new OdfDriver() {
            @Override
            public IOPool<?> getIOPool() {
                return getTestConfig().getIOPoolProvider().getIOPool();
            }
        };
    }
}