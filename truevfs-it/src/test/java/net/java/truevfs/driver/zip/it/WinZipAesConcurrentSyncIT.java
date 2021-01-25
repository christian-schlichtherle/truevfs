/*
 * Copyright © 2005 - 2021 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.driver.zip.it;

import java.io.IOException;
import net.java.truevfs.access.TConfig;
import net.java.truevfs.access.it.ConcurrentSyncITSuite;
import net.java.truevfs.comp.zipdriver.TestWinZipAesDriver;
import static net.java.truevfs.kernel.spec.FsAccessOption.ENCRYPT;

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
