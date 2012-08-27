/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.driver.zip.sample;

import java.nio.charset.Charset;
import net.java.truevfs.access.TArchiveDetector;
import net.java.truevfs.access.TConfig;
import net.java.truevfs.comp.zipdriver.KeyManagementITSuite;

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
