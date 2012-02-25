/*
 * Copyright (C) 2004-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.fs.archive.zip.raes.sample;

import de.schlichtherle.truezip.file.TArchiveDetector;
import de.schlichtherle.truezip.file.TFile;
import de.schlichtherle.truezip.fs.archive.zip.sample.KeyManagementTestBase;

/**
 * @author Christian Schlichtherle
 */
public final class KeyManagementIT extends KeyManagementTestBase {

    @Override
    protected TArchiveDetector newArchiveDetector1(String suffix, String password) {
        return KeyManagement.newArchiveDetector1(
                TFile.getDefaultArchiveDetector(),
                suffix,
                password.toCharArray());
    }

    @Override
    protected TArchiveDetector newArchiveDetector2(String suffix, String password) {
        return KeyManagement.newArchiveDetector2(
                TFile.getDefaultArchiveDetector(),
                suffix,
                password.toCharArray());
    }
}
