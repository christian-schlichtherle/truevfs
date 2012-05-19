/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.truevfs.driver.tar;

import net.truevfs.kernel.FsArchiveDriverTestSuite;
import net.truevfs.kernel.cio.IOPool;

/**
 * @author Christian Schlichtherle
 */
public final class TarDriverTest
extends FsArchiveDriverTestSuite<TarDriverEntry, TarDriver> {

    @Override
    protected TarDriver newArchiveDriver() {
        return new TarDriver() {
            @Override
            public IOPool<?> getIOPool() {
                return getTestConfig().getIOPoolProvider().getIOPool();
            }
        };
    }

    @Override
    protected String getUnencodableName() {
        return null;
    }
}
