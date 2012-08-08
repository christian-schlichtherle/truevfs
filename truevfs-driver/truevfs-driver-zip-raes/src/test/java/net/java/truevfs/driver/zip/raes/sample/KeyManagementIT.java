/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.driver.zip.raes.sample;

import net.java.truevfs.access.TArchiveDetector;
import net.java.truevfs.access.TConfig;
import net.java.truevfs.comp.zip.driver.KeyManagementITSuite;

/**
 * @author Christian Schlichtherle
 */
public final class KeyManagementIT extends KeyManagementITSuite {

    @Override
    protected TArchiveDetector newArchiveDetector1(String extension, String password) {
        return KeyManagement.newArchiveDetector1(
                TConfig.get().getArchiveDetector(),
                extension,
                password.toCharArray());
    }

    @Override
    protected TArchiveDetector newArchiveDetector2(String extension, String password) {
        return KeyManagement.newArchiveDetector2(
                TConfig.get().getArchiveDetector(),
                extension,
                password.toCharArray());
    }
}
