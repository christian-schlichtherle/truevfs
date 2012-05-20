/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.truevfs.driver.zip.it;

import net.truevfs.driver.zip.CheckedZipDriver;
import net.truevfs.access.TFileITSuite;
import net.truevfs.kernel.cio.IOPool;

/**
 * @author  Christian Schlichtherle
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
            public IOPool<?> getIOPool() {
                return getTestConfig().getIOPoolProvider().getIOPool();
            }
        };
    }
}