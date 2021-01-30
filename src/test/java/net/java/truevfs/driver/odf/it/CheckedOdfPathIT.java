/*
 * Copyright © 2005 - 2021 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.driver.odf.it;

import net.java.truevfs.access.it.TPathITSuite;
import net.java.truevfs.driver.odf.CheckedOdfDriver;
import net.java.truevfs.kernel.spec.FsTestConfig;
import net.java.truecommons.cio.IoBufferPool;

/**
 * @author Christian Schlichtherle
 */
public final class CheckedOdfPathIT extends TPathITSuite<CheckedOdfDriver> {

    @Override
    protected String getExtensionList() {
        return "odf";
    }

    @Override
    protected CheckedOdfDriver newArchiveDriver() {
        return new CheckedOdfDriver() {
            @Override
            public IoBufferPool getPool() {
                return FsTestConfig.get().getPool();
            }
        };
    }
}
