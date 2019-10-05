/*
 * Copyright (C) 2005-2015 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.comp.zipdriver;

import net.java.truevfs.kernel.spec.FsArchiveDriverTestSuite;

/**
 * @author Christian Schlichtherle
 */
public final class WinZipAesDriverTest extends FsArchiveDriverTestSuite<ZipDriverEntry, ZipDriver> {

    @Override
    protected ZipDriver newArchiveDriver() {
        return new TestWinZipAesDriver();
    }

    @Override
    protected String getUnencodableName() {
        return "\u2297";
    }
}
