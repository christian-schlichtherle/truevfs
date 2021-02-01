/*
 * Copyright © 2005 - 2021 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package global.namespace.truevfs.driver.odf;

import global.namespace.truevfs.comp.cio.IoBufferPool;
import global.namespace.truevfs.comp.zipdriver.JarDriverEntry;
import global.namespace.truevfs.kernel.spec.FsArchiveDriverTestSuite;
import global.namespace.truevfs.kernel.spec.FsTestConfig;

/**
 * @author Christian Schlichtherle
 */
public final class CheckedOdfDriverTest extends FsArchiveDriverTestSuite<JarDriverEntry, CheckedOdfDriver> {

    @Override
    protected CheckedOdfDriver newArchiveDriver() {
        return new CheckedOdfDriver() {
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
