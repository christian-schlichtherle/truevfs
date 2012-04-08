/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.file.zip;

import de.schlichtherle.truezip.file.TFileITSuite;
import de.schlichtherle.truezip.fs.archive.zip.ZipDriver;

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
