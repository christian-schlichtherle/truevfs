/*
 * Copyright © 2005 - 2021 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package global.namespace.truevfs.commons.zipdriver;

import global.namespace.truevfs.it.base.FsArchiveDriverTestSuite;

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
