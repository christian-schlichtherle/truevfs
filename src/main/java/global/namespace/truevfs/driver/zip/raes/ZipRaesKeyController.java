/*
 * Copyright © 2005 - 2021 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package global.namespace.truevfs.driver.zip.raes;

import global.namespace.truevfs.comp.key.spec.common.AesPbeParameters;
import global.namespace.truevfs.comp.zipdriver.AbstractKeyController;
import global.namespace.truevfs.driver.zip.raes.crypto.RaesKeyException;
import global.namespace.truevfs.kernel.api.FsController;

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
