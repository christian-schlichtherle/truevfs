/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.truezip.driver.tar;

import de.truezip.kernel.FsCharsetArchiveDriverTestSuite;
import de.truezip.driver.tar.TarDriver;
import de.truezip.driver.tar.TarDriverEntry;

/**
 * @author Christian Schlichtherle
 */
public final class TarDriverTest
extends FsCharsetArchiveDriverTestSuite<TarDriverEntry, TarDriver> {

    @Override
    protected TarDriver newArchiveDriver() {
        return new TarDriver(getTestConfig().getIOPoolProvider());
    }

    @Override
    protected String getUnencodableName() {
        return "\u0080";
    }
}
