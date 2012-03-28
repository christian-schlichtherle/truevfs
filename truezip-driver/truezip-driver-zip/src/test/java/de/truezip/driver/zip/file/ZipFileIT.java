/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.truezip.driver.zip.file;

import de.truezip.driver.zip.ZipDriver;
import de.truezip.file.TFileITSuite;

/**
 * @author  Christian Schlichtherle
 */
public final class ZipFileIT extends TFileITSuite<ZipDriver> {

    @Override
    protected String getSuffixList() {
        return "zip";
    }

    @Override
    protected ZipDriver newArchiveDriver() {
        return new ZipDriver(getTestConfig().getIOPoolProvider());
    }
}