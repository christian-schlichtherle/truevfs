/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.file.tar;

import de.schlichtherle.truezip.file.ConcurrentSyncTestSuite;
import de.schlichtherle.truezip.fs.archive.tar.TarBZip2Driver;

/**
 * @author Christian Schlichtherle
 */
public final class TarBZip2ConcurrentSyncIT
extends ConcurrentSyncTestSuite<TarBZip2Driver> {

    @Override
    protected String getSuffixList() {
        return "tar.bz2";
    }

    @Override
    protected TarBZip2Driver newArchiveDriver() {
        return new TarBZip2Driver(getTestConfig().getIOPoolProvider());
    }
}
