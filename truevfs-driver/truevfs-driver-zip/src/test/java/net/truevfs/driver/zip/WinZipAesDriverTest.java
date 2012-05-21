/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.truevfs.driver.zip;

import net.truevfs.kernel.FsArchiveDriverTestSuite;

/**
 * @author Christian Schlichtherle
 */
public final class WinZipAesDriverTest
extends FsArchiveDriverTestSuite<ZipDriverEntry, ZipDriver> {

    @Override
    protected ZipDriver newArchiveDriver() {
        return new TestWinZipAesDriver(getTestConfig().getIoPoolProvider());
    }

    @Override
    protected String getUnencodableName() {
        return "\u2297";
    }
}
