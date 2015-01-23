/*
 * Copyright (C) 2005-2015 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.driver.zip.it;

import net.java.truecommons.cio.IoBufferPool;
import net.java.truevfs.access.it.TFileITSuite;
import net.java.truevfs.comp.zipdriver.ZipDriver;
import net.java.truevfs.kernel.spec.FsTestConfig;

/**
 * @author Christian Schlichtherle
 */
public final class ZipFileIT extends TFileITSuite<ZipDriver> {

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
