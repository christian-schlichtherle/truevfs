/*
 * Copyright © 2005 - 2021 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package global.namespace.truevfs.driver.tar.it;

import global.namespace.truevfs.comp.tardriver.TarDriver;
import global.namespace.truevfs.comp.tardriver.it.TarPathITSuite;
import global.namespace.truevfs.kernel.spec.FsTestConfig;
import global.namespace.truevfs.comp.cio.IoBufferPool;

/**
 * @author Christian Schlichtherle
 */
public final class TarPathIT extends TarPathITSuite<TarDriver> {
    @Override
    protected String getExtensionList() {
        return "tar";
    }

    @Override
    protected TarDriver newArchiveDriver() {
        return new TarDriver() {
            @Override
            public IoBufferPool getPool() {
                return FsTestConfig.get().getPool();
            }
        };
    }
}
