/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.fs.archive.zip;

import de.schlichtherle.truezip.fs.FsCharsetArchiveDriverTestSuite;

/**
 * @author Christian Schlichtherle
 */
public final class OdfDriverTest
extends FsCharsetArchiveDriverTestSuite<ZipDriverEntry, JarDriver> {

    @Override
    protected JarDriver newArchiveDriver() {
        return new OdfDriver(getTestConfig().getIOPoolProvider());
    }

    @Override
    protected String getUnencodableName() {
        return null;
    }
}
