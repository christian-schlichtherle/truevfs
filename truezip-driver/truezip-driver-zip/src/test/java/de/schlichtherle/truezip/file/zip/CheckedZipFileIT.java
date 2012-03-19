/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.file.zip;

import de.schlichtherle.truezip.file.TFileTestSuite;
import de.schlichtherle.truezip.fs.archive.zip.CheckedZipDriver;

/**
 * @author  Christian Schlichtherle
 */
public final class CheckedZipFileIT extends TFileTestSuite<CheckedZipDriver> {

    @Override
    protected String getSuffixList() {
        return "zip";
    }

    @Override
    protected CheckedZipDriver newArchiveDriver() {
        return new CheckedZipDriver(getTestConfig().getIOPoolProvider());
    }
}