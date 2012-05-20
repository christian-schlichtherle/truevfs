/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.truevfs.driver.tar.it;

import net.truevfs.driver.tar.TarGZipDriver;
import net.truevfs.kernel.cio.IOPool;

/**
 * @author  Christian Schlichtherle
 */
public final class TarGZipPathIT extends TarPathITSuite<TarGZipDriver> {

    @Override
    protected String getExtensionList() {
        return "tar.gz";
    }

    @Override
    protected TarGZipDriver newArchiveDriver() {
        return new TarGZipDriver() {
            @Override
            public IOPool<?> getIOPool() {
                return getTestConfig().getIOPoolProvider().getIOPool();
            }
        };
    }
}