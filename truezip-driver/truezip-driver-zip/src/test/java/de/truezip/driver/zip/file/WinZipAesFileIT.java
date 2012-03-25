/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.truezip.driver.zip.file;

import de.truezip.file.TFileITSuite;
import de.truezip.driver.zip.TestWinZipAesDriver;

/**
 * @author  Christian Schlichtherle
 */
public final class WinZipAesFileIT extends TFileITSuite<TestWinZipAesDriver> {
    @Override
    protected String getSuffixList() {
        return "zip";
    }

    @Override
    protected TestWinZipAesDriver newArchiveDriver() {
        return new TestWinZipAesDriver(getTestConfig().getIOPoolProvider());
    }
}