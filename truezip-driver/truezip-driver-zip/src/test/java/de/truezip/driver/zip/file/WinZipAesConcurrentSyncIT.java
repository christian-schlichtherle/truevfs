/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.truezip.driver.zip.file;

import de.truezip.driver.zip.TestWinZipAesDriver;
import de.truezip.file.ConcurrentSyncITSuite;
import de.truezip.file.TConfig;
import static de.truezip.kernel.FsAccessOption.ENCRYPT;
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