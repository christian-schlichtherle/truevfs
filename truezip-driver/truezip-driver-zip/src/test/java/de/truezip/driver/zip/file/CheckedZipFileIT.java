/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.truezip.driver.zip.file;

import de.truezip.driver.zip.CheckedZipDriver;
import de.truezip.file.TFileITSuite;

/**
 * @author  Christian Schlichtherle
 */
public final class CheckedZipFileIT extends TFileITSuite<CheckedZipDriver> {

    @Override
    protected String getSuffixList() {
        return "zip";
    }

    @Override
    protected CheckedZipDriver newArchiveDriver() {
        return new CheckedZipDriver(getTestConfig().getIOPoolProvider());
    }
}