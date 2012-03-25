/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.truezip.driver.zip.path;

import de.truezip.driver.zip.ZipDriver;
import de.truezip.path.TPathTestSuite;

/**
 * @author  Christian Schlichtherle
 */
public final class ZipPathIT extends TPathTestSuite<ZipDriver> {

    @Override
    protected String getSuffixList() {
        return "zip";
    }

    @Override
    protected ZipDriver newArchiveDriver() {
        return new ZipDriver(getTestConfig().getIOPoolProvider());
    }
}