/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.nio.file.zip;

import de.schlichtherle.truezip.fs.archive.zip.CheckedZipDriver;
import de.schlichtherle.truezip.nio.file.TPathTestSuite;

/**
 * @author  Christian Schlichtherle
 */
public final class CheckedZipPathIT extends TPathTestSuite<CheckedZipDriver> {

    @Override
    protected String getSuffixList() {
        return "zip";
    }

    @Override
    protected CheckedZipDriver newArchiveDriver() {
        return new CheckedZipDriver(getTestConfig().getIOPoolProvider());
    }
}