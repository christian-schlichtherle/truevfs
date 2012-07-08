/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.nio.file.tar;

import de.schlichtherle.truezip.fs.archive.tar.TarGZipDriver;
import de.schlichtherle.truezip.fs.archive.tar.TestTarGZipDriver;

/**
 * @author Christian Schlichtherle
 */
public final class TarGZipPathIT extends TarPathITSuite<TarGZipDriver> {

    @Override
    protected String getSuffixList() {
        return "tar.gz";
    }

    @Override
    protected TarGZipDriver newArchiveDriver() {
        return new TestTarGZipDriver();
    }
}
