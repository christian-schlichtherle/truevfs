/*
 * Copyright (C) 2004-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.fs.archive.zip.sample;

import de.schlichtherle.truezip.file.TArchiveDetector;
import de.schlichtherle.truezip.file.TFile;
import java.nio.charset.Charset;

/**
 * @author Christian Schlichtherle
 */
public final class KeyManagementIT extends KeyManagementTestBase {

    private static final Charset US_ASCII = Charset.forName("US-ASCII");

    @Override
    protected TArchiveDetector newArchiveDetector1(String suffix, String password) {
        return KeyManagement.newArchiveDetector1(
                TFile.getDefaultArchiveDetector(),
                suffix,
                password.getBytes(US_ASCII));
    }

    @Override
    protected TArchiveDetector newArchiveDetector2(String suffix, String password) {
        return KeyManagement.newArchiveDetector2(
                TFile.getDefaultArchiveDetector(),
                suffix,
                password.toCharArray());
    }
}
