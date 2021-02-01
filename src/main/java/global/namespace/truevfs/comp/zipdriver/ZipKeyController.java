/*
 * Copyright © 2005 - 2021 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package global.namespace.truevfs.comp.zipdriver;

import global.namespace.truevfs.comp.key.spec.common.AesPbeParameters;
import global.namespace.truevfs.comp.zip.ZipKeyException;
import global.namespace.truevfs.kernel.spec.FsController;

import java.io.IOException;

/**
 * This file system controller decorates another file system controller in
 * order to manage its AES PBE parameters.
 *
 * @author Christian Schlichtherle
 */
final class ZipKeyController extends AbstractKeyController {

    ZipKeyController(FsController controller, AbstractZipDriver<?> driver) {
        super(controller, driver);
    }

    @Override
    protected Class<?> getKeyType() {
        return AesPbeParameters.class;
    }

    @Override
    protected Class<? extends IOException> getKeyExceptionType() {
        return ZipKeyException.class;
    }
}
