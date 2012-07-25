/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.truevfs.driver.tar.gzip;

import net.truevfs.component.tar.driver.TarDriverEntry;
import net.truevfs.kernel.spec.FsArchiveDriverTestSuite;

/**
 * @author Christian Schlichtherle
 */
public final class TarGZipDriverTest
extends FsArchiveDriverTestSuite<TarDriverEntry, TarGZipDriver> {
    @Override
    protected TarGZipDriver newArchiveDriver() {
        return new TestTarGZipDriver();
    }

    @Override
    protected String getUnencodableName() {
        return null;
    }
}
