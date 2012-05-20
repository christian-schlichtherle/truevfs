/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.truevfs.driver.zip.raes.it;

import net.truevfs.driver.zip.raes.TestZipRaesDriver;
import net.truevfs.access.ConcurrentSyncITSuite;

/**
 * @author Christian Schlichtherle
 */
public final class ZipRaesConcurrentSyncIT extends ConcurrentSyncITSuite<TestZipRaesDriver> {

    @Override
    protected String getExtensionList() {
        return "tzp";
    }

    @Override
    protected TestZipRaesDriver newArchiveDriver() {
        return new TestZipRaesDriver(getTestConfig().getIOPoolProvider());
    }
}
