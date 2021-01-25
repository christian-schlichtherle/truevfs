/*
 * Copyright © 2005 - 2021 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.driver.tar.it;

import net.java.truecommons.cio.IoBufferPool;
import net.java.truevfs.comp.tardriver.TarDriver;
import net.java.truevfs.comp.tardriver.it.TarFileITSuite;
import net.java.truevfs.kernel.spec.FsTestConfig;

/**
 * @author Christian Schlichtherle
 */
public final class TarFileIT extends TarFileITSuite<TarDriver> {
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
