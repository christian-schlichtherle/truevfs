/*
 * Copyright (C) 2004-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.file.tar;

import de.schlichtherle.truezip.fs.archive.tar.TarDriver;

/**
 * @author Christian Schlichtherle
 */
public final class TarFileIT extends TarFileTestSuite<TarDriver> {

    @Override
    protected String getSuffixList() {
        return "tar";
    }

    @Override
    protected TarDriver newArchiveDriver() {
        return new TarDriver(getTestConfig().getIOPoolProvider());
    }
}
