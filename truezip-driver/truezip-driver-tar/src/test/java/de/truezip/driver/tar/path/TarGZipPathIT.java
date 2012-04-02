/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.truezip.driver.tar.path;

import de.truezip.driver.tar.TarGZipDriver;

/**
 * @author  Christian Schlichtherle
 */
public final class TarGZipPathIT extends TarPathITSuite<TarGZipDriver> {

    @Override
    protected String getExtensionList() {
        return "tar.gz";
    }

    @Override
    protected TarGZipDriver newArchiveDriver() {
        return new TarGZipDriver(getTestConfig().getIOPoolProvider());
    }
}