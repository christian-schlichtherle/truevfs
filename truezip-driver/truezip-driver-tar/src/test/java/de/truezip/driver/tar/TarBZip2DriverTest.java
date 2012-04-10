/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.truezip.driver.tar;

import de.truezip.kernel.FsArchiveDriverTestSuite;
import de.truezip.kernel.cio.IOPool;

/**
 * @author Christian Schlichtherle
 */
public final class TarBZip2DriverTest
extends FsArchiveDriverTestSuite<TarDriverEntry, TarBZip2Driver> {

    @Override
    protected TarBZip2Driver newArchiveDriver() {
        return new TarBZip2Driver() {
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
