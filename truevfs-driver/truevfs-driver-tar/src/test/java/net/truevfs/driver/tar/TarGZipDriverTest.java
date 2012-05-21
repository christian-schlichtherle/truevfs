/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.truevfs.driver.tar;

import net.truevfs.kernel.FsArchiveDriverTestSuite;
import net.truevfs.kernel.cio.IoPool;

/**
 * @author Christian Schlichtherle
 */
public final class TarGZipDriverTest
extends FsArchiveDriverTestSuite<TarDriverEntry, TarGZipDriver> {

    @Override
    protected TarGZipDriver newArchiveDriver() {
        return new TarGZipDriver() {
            @Override
            public IoPool<?> getIoPool() {
                return getTestConfig().getIoPoolProvider().getIoPool();
            }
        };
    }

    @Override
    protected String getUnencodableName() {
        return null;
    }
}
