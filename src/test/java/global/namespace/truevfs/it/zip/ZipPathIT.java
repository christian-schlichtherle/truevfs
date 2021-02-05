/*
 * Copyright © 2005 - 2021 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package global.namespace.truevfs.it.zip;

import global.namespace.truevfs.commons.cio.IoBufferPool;
import global.namespace.truevfs.commons.zipdriver.ZipDriver;
import global.namespace.truevfs.it.base.TPathITSuite;
import global.namespace.truevfs.kernel.api.FsTestConfig;

/**
 * @author Christian Schlichtherle
 */
public final class ZipPathIT extends TPathITSuite<ZipDriver> {

    @Override
    protected String getExtensionList() { return "zip"; }

    @Override
    protected ZipDriver newArchiveDriver() {
        return new ZipDriver() {
            @Override
            public IoBufferPool getPool() {
                return FsTestConfig.get().getPool();
            }
        };
    }
}
