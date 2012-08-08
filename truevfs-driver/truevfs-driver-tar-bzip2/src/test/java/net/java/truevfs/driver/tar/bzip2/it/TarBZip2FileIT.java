/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.driver.tar.bzip2.it;

import net.java.truevfs.comp.tar.driver.it.TarFileITSuite;
import net.java.truevfs.driver.tar.bzip2.TarBZip2Driver;
import net.java.truevfs.driver.tar.bzip2.TestTarBZip2Driver;

/**
 * @author Christian Schlichtherle
 */
public final class TarBZip2FileIT extends TarFileITSuite<TarBZip2Driver> {
    @Override
    protected String getExtensionList() {
        return "tar.bz2";
    }

    @Override
    protected TarBZip2Driver newArchiveDriver() {
        return new TestTarBZip2Driver();
    }
}
