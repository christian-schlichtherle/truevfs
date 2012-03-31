/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.truezip.driver.zip;

import de.truezip.kernel.FsCharsetArchiveDriverTestSuite;
import de.truezip.driver.zip.CheckedOdfDriver;
import de.truezip.driver.zip.ZipDriverEntry;

/**
 * @author Christian Schlichtherle
 */
public final class CheckedOdfDriverTest
extends FsCharsetArchiveDriverTestSuite<ZipDriverEntry, CheckedOdfDriver> {

    @Override
    protected CheckedOdfDriver newArchiveDriver() {
        return new CheckedOdfDriver(getTestConfig().getIOPoolProvider());
    }

    @Override
    protected String getUnencodableName() {
        return null;
    }
}
