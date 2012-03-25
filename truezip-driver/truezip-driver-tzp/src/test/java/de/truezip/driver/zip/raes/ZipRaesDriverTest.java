/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.truezip.driver.zip.raes;

import de.schlichtherle.truezip.fs.FsCharsetArchiveDriverTestSuite;
import de.truezip.driver.zip.ZipDriverEntry;

/**
 * @author  Christian Schlichtherle
 */
public final class ZipRaesDriverTest
extends FsCharsetArchiveDriverTestSuite<ZipDriverEntry, ZipRaesDriver> {

    @Override
    protected TestZipRaesDriver newArchiveDriver() {
        return new TestZipRaesDriver(getTestConfig().getIOPoolProvider());
    }

    @Override
    protected String getUnencodableName() {
        return null;
    }
}