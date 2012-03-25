/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.truezip.driver.tar.it.file;

import de.truezip.driver.tar.TarGZipDriver;

/**
 * @author Christian Schlichtherle
 */
public final class TarGZipFileIT extends TarFileITSuite<TarGZipDriver> {

    @Override
    protected String getSuffixList() {
        return "tar.gz";
    }

    @Override
    protected TarGZipDriver newArchiveDriver() {
        return new TarGZipDriver(getTestConfig().getIOPoolProvider());
    }
}
