/*
 * Copyright © 2005 - 2021 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package global.namespace.truevfs.driver.zip.it;

import global.namespace.truevfs.comp.cio.IoBufferPool;
import global.namespace.truevfs.access.it.ConcurrentSyncITSuite;
import global.namespace.truevfs.comp.zipdriver.ZipDriver;
import global.namespace.truevfs.kernel.spec.FsTestConfig;

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
                return FsTestConfig.get().getPool();
            }
        };
    }
}
