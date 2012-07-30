/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.component.zip.driver;

import net.java.truevfs.component.zip.driver.ZipDriver;
import net.java.truevfs.component.zip.driver.ZipDriverEntry;
import net.java.truevfs.kernel.spec.FsArchiveDriverTestSuite;

/**
 * @author Christian Schlichtherle
 */
public final class WinZipAesDriverTest
extends FsArchiveDriverTestSuite<ZipDriverEntry, ZipDriver> {

    @Override
    protected ZipDriver newArchiveDriver() {
        return new TestWinZipAesDriver();
    }

    @Override
    protected String getUnencodableName() {
        return "\u2297";
    }
}
