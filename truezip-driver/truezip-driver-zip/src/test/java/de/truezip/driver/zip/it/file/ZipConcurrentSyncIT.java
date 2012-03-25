/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.truezip.driver.zip.it.file;

import de.schlichtherle.truezip.file.ConcurrentSyncTestSuite;
import de.truezip.driver.zip.ZipDriver;

/**
 * @author  Christian Schlichtherle
 */
public final class ZipConcurrentSyncIT extends ConcurrentSyncTestSuite<ZipDriver> {

    @Override
    protected String getSuffixList() {
        return "zip";
    }

    @Override
    protected ZipDriver newArchiveDriver() {
        return new ZipDriver(getTestConfig().getIOPoolProvider());
    }
}