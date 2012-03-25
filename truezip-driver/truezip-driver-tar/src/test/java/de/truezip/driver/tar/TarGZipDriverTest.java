/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.truezip.driver.tar;

import de.schlichtherle.truezip.fs.FsCharsetArchiveDriverTestSuite;
import de.truezip.driver.tar.TarDriverEntry;
import de.truezip.driver.tar.TarGZipDriver;

/**
 * @author Christian Schlichtherle
 */
public final class TarGZipDriverTest
extends FsCharsetArchiveDriverTestSuite<TarDriverEntry, TarGZipDriver> {

    @Override
    protected TarGZipDriver newArchiveDriver() {
        return new TarGZipDriver(getTestConfig().getIOPoolProvider());
    }

    @Override
    protected String getUnencodableName() {
        return "\u0080";
    }
}
