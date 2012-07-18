/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.fs.archive.tar;

import de.schlichtherle.truezip.fs.FsCharsetArchiveDriverTestSuite;

/**
 * @author Christian Schlichtherle
 */
public final class TarGZipDriverTest
extends FsCharsetArchiveDriverTestSuite<TarDriverEntry, TarGZipDriver> {

    @Override
    protected TarGZipDriver newArchiveDriver() {
        return new TestTarGZipDriver();
    }

    @Override
    protected String getUnencodableName() {
        return "\u0080";
    }
}