/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.driver.tar.gzip.it;

import net.java.truevfs.comp.tardriver.it.TarFileITSuite;
import net.java.truevfs.driver.tar.gzip.TarGZipDriver;
import net.java.truevfs.driver.tar.gzip.TestTarGZipDriver;

/**
 * @author Christian Schlichtherle
 */
public final class TarGZipFileIT extends TarFileITSuite<TarGZipDriver> {
    @Override
    protected String getExtensionList() {
        return "tar.gz";
    }

    @Override
    protected TarGZipDriver newArchiveDriver() {
        return new TestTarGZipDriver();
    }
}
