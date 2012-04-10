/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.truezip.driver.zip;

import de.truezip.kernel.FsArchiveDriverTestSuite;
import de.truezip.kernel.cio.IOPool;

/**
 * @author Christian Schlichtherle
 */
public final class CheckedJarDriverTest
extends FsArchiveDriverTestSuite<ZipDriverEntry, CheckedJarDriver> {

    @Override
    protected CheckedJarDriver newArchiveDriver() {
        return new CheckedJarDriver() {
            @Override
            public IOPool<?> getIOPool() {
                return getTestConfig().getIOPoolProvider().get();
            }
        };
    }

    @Override
    protected String getUnencodableName() {
        return null;
    }
}
