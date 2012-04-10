/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.truezip.driver.zip.path;

import de.truezip.driver.zip.ZipDriver;
import de.truezip.kernel.cio.IOPool;
import de.truezip.path.TPathITSuite;

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
                return getTestConfig().getIOPoolProvider().get();
            }
        };
    }
}