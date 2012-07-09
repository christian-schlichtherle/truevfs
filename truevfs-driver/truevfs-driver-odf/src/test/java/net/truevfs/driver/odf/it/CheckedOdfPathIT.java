/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.truevfs.driver.odf.it;

import net.truevfs.access.TPathITSuite;
import net.truevfs.driver.odf.CheckedOdfDriver;
import net.truevfs.kernel.spec.cio.IoPool;

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
            public IoPool<?> getIoPool() {
                return getTestConfig().getIoPoolProvider().getIoPool();
            }
        };
    }
}
