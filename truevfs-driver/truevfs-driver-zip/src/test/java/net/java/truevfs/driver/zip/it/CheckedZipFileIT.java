/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.driver.zip.it;

import net.java.truevfs.access.TFileITSuite;
import net.java.truevfs.comp.zipdriver.CheckedZipDriver;
import net.java.truevfs.kernel.spec.TestConfig;
import net.java.truevfs.kernel.spec.cio.IoBufferPool;

/**
 * @author Christian Schlichtherle
 */
public final class CheckedZipFileIT extends TFileITSuite<CheckedZipDriver> {

    @Override
    protected String getExtensionList() {
        return "zip";
    }

    @Override
    protected CheckedZipDriver newArchiveDriver() {
        return new CheckedZipDriver() {
            @Override
            public IoBufferPool getPool() {
                return TestConfig.get().getPool();
            }
        };
    }
}
