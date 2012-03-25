/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.truezip.driver.zip.it.path;

import de.truezip.driver.zip.TestWinZipAesDriver;
import de.schlichtherle.truezip.nio.file.TPathTestSuite;

/**
 * @author  Christian Schlichtherle
 */
public final class WinZipAesPathIT extends TPathTestSuite<TestWinZipAesDriver> {
    @Override
    protected String getSuffixList() {
        return "zip";
    }

    @Override
    protected TestWinZipAesDriver newArchiveDriver() {
        return new TestWinZipAesDriver( getTestConfig().getIOPoolProvider());
    }
}