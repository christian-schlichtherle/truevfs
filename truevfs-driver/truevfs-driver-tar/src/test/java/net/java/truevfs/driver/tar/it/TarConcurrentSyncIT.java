/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.driver.tar.it;

import net.java.truevfs.access.it.ConcurrentSyncITSuite;
import net.java.truevfs.comp.tardriver.TarDriver;
import net.java.truevfs.kernel.spec.TestConfig;
import net.java.truevfs.kernel.spec.cio.IoBufferPool;

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
            public IoBufferPool getPool() {
                return TestConfig.get().getPool();
            }
        };
    }
}