/*
 * Copyright © 2005 - 2021 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package global.namespace.truevfs.commons.tardriver;

import global.namespace.truevfs.commons.cio.IoBufferPool;
import global.namespace.truevfs.it.base.FsArchiveDriverTestSuite;
import global.namespace.truevfs.kernel.api.FsTestConfig;

/**
 * @author Christian Schlichtherle
 */
public final class TarDriverTest extends FsArchiveDriverTestSuite<TarDriverEntry, TarDriver> {

    @Override
    protected TarDriver newArchiveDriver() {
        return new TarDriver() {
            @Override
            public IoBufferPool getPool() {
                return FsTestConfig.get().getPool();
            }
        };
    }

    @Override
    protected String getUnencodableName() {
        return null;
    }
}
