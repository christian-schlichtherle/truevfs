/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.truezip.driver.zip;

import de.schlichtherle.truezip.fs.FsCharsetArchiveDriverTestSuite;
import de.truezip.driver.zip.CheckedJarDriver;
import de.truezip.driver.zip.ZipDriverEntry;

/**
 * @author Christian Schlichtherle
 */
public final class CheckedJarDriverTest
extends FsCharsetArchiveDriverTestSuite<ZipDriverEntry, CheckedJarDriver> {

    @Override
    protected CheckedJarDriver newArchiveDriver() {
        return new CheckedJarDriver(getTestConfig().getIOPoolProvider());
    }

    @Override
    protected String getUnencodableName() {
        return null;
    }
}
