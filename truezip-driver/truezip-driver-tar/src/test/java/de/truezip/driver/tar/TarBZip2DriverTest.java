/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.truezip.driver.tar;

import de.truezip.kernel.fs.FsCharsetArchiveDriverTestSuite;
import de.truezip.driver.tar.TarBZip2Driver;
import de.truezip.driver.tar.TarDriverEntry;

/**
 * @author Christian Schlichtherle
 */
public final class TarBZip2DriverTest
extends FsCharsetArchiveDriverTestSuite<TarDriverEntry, TarBZip2Driver> {

    @Override
    protected TarBZip2Driver newArchiveDriver() {
        return new TarBZip2Driver(getTestConfig().getIOPoolProvider());
    }

    @Override
    protected String getUnencodableName() {
        return "\u0080";
    }
}
