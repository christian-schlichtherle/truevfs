package net.truevfs.driver.zip.it;

/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */


import net.truevfs.access.TPathITSuite;
import net.truevfs.driver.zip.CheckedZipDriver;
import net.truevfs.kernel.cio.IoPool;

/**
 * @author  Christian Schlichtherle
 */
public final class CheckedZipPathIT extends TPathITSuite<CheckedZipDriver> {

    @Override
    protected String getExtensionList() {
        return "zip";
    }

    @Override
    protected CheckedZipDriver newArchiveDriver() {
        return new CheckedZipDriver() {
            @Override
            public IoPool<?> getIoPool() {
                return getTestConfig().getIoPoolProvider().getIoPool();
            }
        };
    }
}