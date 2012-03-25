/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.truezip.driver.tar.it.file;

import de.truezip.file.ConcurrentSyncITSuite;
import de.truezip.driver.tar.TarBZip2Driver;

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
        return new TarBZip2Driver(getTestConfig().getIOPoolProvider());
    }
}
