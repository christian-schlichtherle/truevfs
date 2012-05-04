/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.truezip.driver.tar.file;

import de.truezip.driver.tar.TarBZip2Driver;
import de.truezip.file.ConcurrentSyncITSuite;
import de.truezip.kernel.cio.IOPool;

/**
 * @author Christian Schlichtherle
 */
public final class TarBZip2ConcurrentSyncIT
extends ConcurrentSyncITSuite<TarBZip2Driver> {

    @Override
    protected String getExtensionList() {
        return "tar.bz2";
    }

    @Override
    protected TarBZip2Driver newArchiveDriver() {
        return new TarBZip2Driver() {
            @Override
            public IOPool<?> getIOPool() {
                return getTestConfig().getIOPoolProvider().getIOPool();
            }
        };
    }
}