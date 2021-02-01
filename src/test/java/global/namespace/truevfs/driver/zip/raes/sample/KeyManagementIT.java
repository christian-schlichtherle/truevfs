/*
 * Copyright © 2005 - 2021 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package global.namespace.truevfs.driver.zip.raes.sample;

import global.namespace.truevfs.access.TArchiveDetector;
import global.namespace.truevfs.access.TConfig;
import global.namespace.truevfs.comp.zipdriver.KeyManagementITSuite;

/**
 * @author Christian Schlichtherle
 */
public final class KeyManagementIT extends KeyManagementITSuite {

    @Override
    protected TArchiveDetector newArchiveDetector1(String extension, String password) {
        return KeyManagement.newArchiveDetector1(
                extension, password.toCharArray(), TConfig.current().getArchiveDetector()
        );
    }

    @Override
    protected TArchiveDetector newArchiveDetector2(String extension, String password) {
        return KeyManagement.newArchiveDetector2(
                extension, password.toCharArray(), TConfig.current().getArchiveDetector()
        );
    }
}
