/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.truevfs.driver.odf.it;

import net.truevfs.access.TFileITSuite;
import net.truevfs.driver.odf.CheckedOdfDriver;
import net.truevfs.kernel.spec.TestConfig;
import net.truevfs.kernel.spec.cio.IoBufferPool;

/**
 * @author Christian Schlichtherle
 */
public final class CheckedOdfFileIT extends TFileITSuite<CheckedOdfDriver> {

    @Override
    protected String getExtensionList() {
        return "odf";
    }

    @Override
    protected CheckedOdfDriver newArchiveDriver() {
        return new CheckedOdfDriver() {
            @Override
            public IoBufferPool<?> getPool() {
                return TestConfig.get().getPool();
            }
        };
    }
}
