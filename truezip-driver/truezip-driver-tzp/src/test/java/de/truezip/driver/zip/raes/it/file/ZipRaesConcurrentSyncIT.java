/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.truezip.driver.zip.raes.it.file;

import de.truezip.file.ConcurrentSyncITSuite;
import de.truezip.driver.zip.raes.TestZipRaesDriver;

/**
 * @author Christian Schlichtherle
 */
public final class ZipRaesConcurrentSyncIT extends ConcurrentSyncITSuite<TestZipRaesDriver> {

    @Override
    protected String getSuffixList() {
        return "tzp";
    }

    @Override
    protected TestZipRaesDriver newArchiveDriver() {
        return new TestZipRaesDriver(getTestConfig().getIOPoolProvider());
    }
}
