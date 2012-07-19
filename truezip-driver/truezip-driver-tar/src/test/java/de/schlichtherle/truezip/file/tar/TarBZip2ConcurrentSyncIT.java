/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.file.tar;

import de.schlichtherle.truezip.file.ConcurrentSyncITSuite;
import de.schlichtherle.truezip.fs.archive.tar.TarBZip2Driver;
import de.schlichtherle.truezip.fs.archive.tar.TestTarBZip2Driver;

/**
 * @author Christian Schlichtherle
 */
public final class TarBZip2ConcurrentSyncIT
extends ConcurrentSyncITSuite<TarBZip2Driver> {

    @Override
    protected String getSuffixList() {
        return "tar.bz2";
    }

    @Override
    protected TarBZip2Driver newArchiveDriver() {
        return new TestTarBZip2Driver();
    }
}
