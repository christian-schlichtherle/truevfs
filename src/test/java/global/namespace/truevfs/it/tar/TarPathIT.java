/*
 * Copyright © 2005 - 2021 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package global.namespace.truevfs.it.tar;

import global.namespace.truevfs.commons.cio.IoBufferPool;
import global.namespace.truevfs.commons.tardriver.TarDriver;
import global.namespace.truevfs.it.tar.base.TarPathITSuite;
import global.namespace.truevfs.kernel.api.FsTestConfig;

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
