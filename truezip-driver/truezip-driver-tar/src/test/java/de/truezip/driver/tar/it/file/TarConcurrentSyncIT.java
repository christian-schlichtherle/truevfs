/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.truezip.driver.tar.it.file;

import de.schlichtherle.truezip.file.ConcurrentSyncTestSuite;
import de.truezip.driver.tar.TarDriver;

/**
 * @author Christian Schlichtherle
 */
public final class TarConcurrentSyncIT
extends ConcurrentSyncTestSuite<TarDriver> {

    @Override
    protected String getSuffixList() {
        return "tar";
    }

    @Override
    protected TarDriver newArchiveDriver() {
        return new TarDriver(getTestConfig().getIOPoolProvider());
    }
}
