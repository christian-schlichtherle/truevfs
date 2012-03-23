/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.fs.archive.zip;

import de.schlichtherle.truezip.fs.FsCharsetArchiveDriverTestSuite;

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
