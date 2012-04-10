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
public final class CheckedOdfDriverTest
extends FsArchiveDriverTestSuite<ZipDriverEntry, CheckedOdfDriver> {

    @Override
    protected CheckedOdfDriver newArchiveDriver() {
        return new CheckedOdfDriver() {
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
