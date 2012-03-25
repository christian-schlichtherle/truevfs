package de.truezip.driver.zip.it.file;

/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */


import de.schlichtherle.truezip.file.ConcurrentSyncTestSuite;
import de.truezip.driver.zip.OdfDriver;

/**
 * @author  Christian Schlichtherle
 */
public final class OdfConcurrentSyncIT extends ConcurrentSyncTestSuite<OdfDriver> {

    @Override
    protected String getSuffixList() {
        return "odf";
    }

    @Override
    protected OdfDriver newArchiveDriver() {
        return new OdfDriver(getTestConfig().getIOPoolProvider());
    }
}