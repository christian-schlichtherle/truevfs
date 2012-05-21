/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.truevfs.driver.zip;

import net.truevfs.kernel.FsArchiveDriverTestSuite;
import net.truevfs.kernel.cio.IOPool;

/**
 * @author Christian Schlichtherle
 */
public final class CheckedJarDriverTest
extends FsArchiveDriverTestSuite<ZipDriverEntry, CheckedJarDriver> {

    @Override
    protected CheckedJarDriver newArchiveDriver() {
        return new CheckedJarDriver() {
            @Override
            public IOPool<?> getIoPool() {
                return getTestConfig().getIoPoolProvider().getIoPool();
            }
        };
    }

    @Override
    protected String getUnencodableName() {
        return null;
    }
}
