/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.fs.archive.tar;

import de.schlichtherle.truezip.fs.FsCharsetArchiveDriverTestSuite;

/**
 * @author Christian Schlichtherle
 */
public final class TarXZDriverTest
extends FsCharsetArchiveDriverTestSuite<TarDriverEntry, TarXZDriver> {

    @Override
    protected TarXZDriver newArchiveDriver() {
        return new TestTarXZDriver();
    }

    @Override
    protected String getUnencodableName() {
        return "\u0080";
    }
}
