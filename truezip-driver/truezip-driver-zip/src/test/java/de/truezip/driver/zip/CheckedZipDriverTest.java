/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.truezip.driver.zip;

import de.truezip.kernel.FsCharsetArchiveDriverTestSuite;
import de.truezip.driver.zip.CheckedZipDriver;
import de.truezip.driver.zip.ZipDriverEntry;

/**
 * @author Christian Schlichtherle
 */
public final class CheckedZipDriverTest
extends FsCharsetArchiveDriverTestSuite<ZipDriverEntry, CheckedZipDriver> {

    @Override
    protected CheckedZipDriver newArchiveDriver() {
        return new CheckedZipDriver(getTestConfig().getIOPoolProvider());
    }

    @Override
    protected String getUnencodableName() {
        return "\u2297";
    }
}
