/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.truevfs.driver.zip.path;

import net.truevfs.driver.zip.ZipDriver;
import net.truevfs.kernel.cio.IOPool;
import net.truevfs.path.TPathITSuite;

/**
 * @author  Christian Schlichtherle
 */
public final class ZipPathIT extends TPathITSuite<ZipDriver> {

    @Override
    protected String getExtensionList() {
        return "zip";
    }

    @Override
    protected ZipDriver newArchiveDriver() {
        return new ZipDriver() {
            @Override
            public IOPool<?> getIOPool() {
                return getTestConfig().getIOPoolProvider().getIOPool();
            }
        };
    }
}