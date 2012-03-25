package de.truezip.driver.zip.file;

/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */


import de.truezip.file.ConcurrentSyncITSuite;
import de.truezip.driver.zip.OdfDriver;

/**
 * @author  Christian Schlichtherle
 */
public final class OdfConcurrentSyncIT extends ConcurrentSyncITSuite<OdfDriver> {

    @Override
    protected String getSuffixList() {
        return "odf";
    }

    @Override
    protected OdfDriver newArchiveDriver() {
        return new OdfDriver(getTestConfig().getIOPoolProvider());
    }
}