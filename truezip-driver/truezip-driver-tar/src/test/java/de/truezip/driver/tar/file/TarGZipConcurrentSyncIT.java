/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.truezip.driver.tar.file;

import de.truezip.driver.tar.TarGZipDriver;
import de.truezip.file.ConcurrentSyncITSuite;
import de.truezip.kernel.cio.IOPool;

/**
 * @author Christian Schlichtherle
 */
public final class TarGZipConcurrentSyncIT
extends ConcurrentSyncITSuite<TarGZipDriver> {

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