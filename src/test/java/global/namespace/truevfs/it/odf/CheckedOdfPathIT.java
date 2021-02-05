/*
 * Copyright © 2005 - 2021 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package global.namespace.truevfs.it.odf;

import global.namespace.truevfs.commons.cio.IoBufferPool;
import global.namespace.truevfs.driver.odf.CheckedOdfDriver;
import global.namespace.truevfs.it.base.TPathITSuite;
import global.namespace.truevfs.kernel.api.FsTestConfig;

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
