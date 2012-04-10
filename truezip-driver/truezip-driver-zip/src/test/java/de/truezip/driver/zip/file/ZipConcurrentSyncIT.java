/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.truezip.driver.zip.file;

import de.truezip.driver.zip.ZipDriver;
import de.truezip.file.ConcurrentSyncITSuite;
import de.truezip.kernel.cio.IOPool;

/**
 * @author  Christian Schlichtherle
 */
public final class ZipConcurrentSyncIT extends ConcurrentSyncITSuite<ZipDriver> {

    @Override
    protected String getExtensionList() {
        return "zip";
    }

    @Override
    protected ZipDriver newArchiveDriver() {
        return new ZipDriver() {
            @Override
            public IOPool<?> getIOPool() {
                return getTestConfig().getIOPoolProvider().get();
            }
        };
    }
}