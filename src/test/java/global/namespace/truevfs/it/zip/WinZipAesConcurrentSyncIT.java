/*
 * Copyright © 2005 - 2021 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package global.namespace.truevfs.it.zip;

import global.namespace.truevfs.access.TConfig;
import global.namespace.truevfs.commons.zipdriver.TestWinZipAesDriver;
import global.namespace.truevfs.it.base.ConcurrentSyncITSuite;

import java.io.IOException;

import static global.namespace.truevfs.kernel.api.FsAccessOption.ENCRYPT;

/**
 * @author Christian Schlichtherle
 */
public final class WinZipAesConcurrentSyncIT extends ConcurrentSyncITSuite<TestWinZipAesDriver> {

    @Override
    protected String getExtensionList() { return "zip"; }

    @Override
    protected TestWinZipAesDriver newArchiveDriver() {
        return new TestWinZipAesDriver();
    }

    @Override
    public void setUp() throws IOException {
        super.setUp();
        TConfig.current().setAccessPreference(ENCRYPT, true);
    }
}
