/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.truevfs.driver.odf.it;

import net.truevfs.access.ConcurrentSyncITSuite;
import net.truevfs.driver.odf.OdfDriver;
import net.truevfs.kernel.spec.cio.IoPool;

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
            public IoPool<?> getIoPool() {
                return getTestConfig().getIoPoolProvider().getIoPool();
            }
        };
    }
}
