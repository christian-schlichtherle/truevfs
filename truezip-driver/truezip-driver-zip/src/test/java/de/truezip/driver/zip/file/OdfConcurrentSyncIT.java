/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.truezip.driver.zip.file;

import de.truezip.driver.zip.OdfDriver;
import de.truezip.file.ConcurrentSyncITSuite;

/**
 * @author Christian Schlichtherle
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