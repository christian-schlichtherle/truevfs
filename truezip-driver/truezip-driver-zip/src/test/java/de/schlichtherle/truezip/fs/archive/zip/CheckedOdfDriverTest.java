/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.fs.archive.zip;

import de.schlichtherle.truezip.fs.archive.FsCharsetArchiveDriverTestSuite;

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
