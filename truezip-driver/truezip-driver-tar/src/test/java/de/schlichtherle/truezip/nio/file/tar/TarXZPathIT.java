/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.nio.file.tar;

import de.schlichtherle.truezip.fs.archive.tar.TarXZDriver;
import de.schlichtherle.truezip.fs.archive.tar.TestTarXZDriver;

/**
 * @author Christian Schlichtherle
 */
public final class TarXZPathIT extends TarPathITSuite<TarXZDriver> {

    @Override
    protected String getSuffixList() {
        return "tar.xz";
    }

    @Override
    protected TarXZDriver newArchiveDriver() {
        return new TestTarXZDriver();
    }
}
