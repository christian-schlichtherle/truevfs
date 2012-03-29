/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.truezip.driver.zip.raes.sample;

import de.truezip.driver.zip.sample.KeyManagementITSuite;
import de.truezip.file.TArchiveDetector;
import de.truezip.file.TConfig;

/**
 * @author Christian Schlichtherle
 */
public final class KeyManagementIT extends KeyManagementITSuite {

    @Override
    protected TArchiveDetector newArchiveDetector1(String suffix, String password) {
        return KeyManagement.newArchiveDetector1(
                TConfig.get().getArchiveDetector(),
                suffix,
                password.toCharArray());
    }

    @Override
    protected TArchiveDetector newArchiveDetector2(String suffix, String password) {
        return KeyManagement.newArchiveDetector2(
                TConfig.get().getArchiveDetector(),
                suffix,
                password.toCharArray());
    }
}