/*
 * Copyright © 2005 - 2021 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.driver.zip.raes;

import net.java.truecommons.key.spec.common.AesPbeParameters;
import net.java.truevfs.comp.zipdriver.AbstractKeyController;
import net.java.truevfs.driver.zip.raes.crypto.RaesKeyException;
import net.java.truevfs.kernel.spec.FsController;

import java.io.IOException;

/**
 * This file system controller decorates another file system controller in
 * order to manage the authentication key required for accessing its
 * RAES encrypted ZIP file (ZIP.RAES).
 *
 * @author Christian Schlichtherle
 */
final class ZipRaesKeyController extends AbstractKeyController {

    ZipRaesKeyController(FsController controller, ZipRaesDriver driver) {
        super(controller, driver);
    }

    @Override
    protected Class<?> getKeyType() {
        return AesPbeParameters.class;
    }

    @Override
    protected Class<? extends IOException> getKeyExceptionType() {
        return RaesKeyException.class;
    }
}
