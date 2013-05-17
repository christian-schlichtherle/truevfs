/*
 * Copyright (C) 2005-2013 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.driver.zip.it;

import net.java.truecommons.cio.IoBufferPool;
import net.java.truevfs.access.it.ConcurrentSyncITSuite;
import net.java.truevfs.comp.zipdriver.ZipDriver;
import net.java.truevfs.kernel.spec.TestConfig;

/**
 * @author Christian Schlichtherle
 */
public final class ZipConcurrentSyncIT
extends ConcurrentSyncITSuite<ZipDriver> {

    @Override
    protected String getExtensionList() { return "zip"; }

    @Override
    protected ZipDriver newArchiveDriver() {
        return new ZipDriver() {
            @Override
            public IoBufferPool getPool() {
                return TestConfig.get().getPool();
            }
        };
    }
}
