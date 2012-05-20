/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.truevfs.driver.zip.access;

import net.truevfs.driver.zip.TestWinZipAesDriver;
import net.truevfs.access.ConcurrentSyncITSuite;
import net.truevfs.access.TConfig;
import static net.truevfs.kernel.FsAccessOption.ENCRYPT;
import java.io.IOException;

/**
 * @author Christian Schlichtherle
 */
public final class WinZipAesConcurrentSyncIT extends ConcurrentSyncITSuite<TestWinZipAesDriver> {

    @Override
    protected String getExtensionList() {
        return "zip";
    }

    @Override
    protected TestWinZipAesDriver newArchiveDriver() {
        return new TestWinZipAesDriver(getTestConfig().getIOPoolProvider());
    }

    @Override
    public void setUp() throws IOException {
        super.setUp();
        final TConfig config = TConfig.get();
        config.setAccessPreferences(config.getAccessPreferences().set(ENCRYPT));
    }
}