/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.truezip.driver.zip.sample;

import de.truezip.file.TArchiveDetector;
import de.truezip.file.TConfig;
import java.nio.charset.Charset;

/**
 * @author Christian Schlichtherle
 */
public final class KeyManagementIT extends KeyManagementITSuite {

    private static final Charset US_ASCII = Charset.forName("US-ASCII");

    @Override
    protected TArchiveDetector newArchiveDetector1(String extension, String password) {
        return KeyManagement.newArchiveDetector1(
                TConfig.get().getArchiveDetector(),
                extension,
                password.getBytes(US_ASCII));
    }

    @Override
    protected TArchiveDetector newArchiveDetector2(String extension, String password) {
        return KeyManagement.newArchiveDetector2(
                TConfig.get().getArchiveDetector(),
                extension,
                password.toCharArray());
    }
}
