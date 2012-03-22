/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.file.zip.raes;

import de.schlichtherle.truezip.file.ConcurrentSyncTestSuite;
import de.schlichtherle.truezip.fs.archive.zip.raes.TestZipRaesDriver;

/**
 * @author Christian Schlichtherle
 */
public final class ZipRaesConcurrentSyncIT extends ConcurrentSyncTestSuite<TestZipRaesDriver> {

    @Override
    protected String getSuffixList() {
        return "tzp";
    }

    @Override
    protected TestZipRaesDriver newArchiveDriver() {
        return new TestZipRaesDriver(getTestConfig().getIOPoolProvider());
    }
}
