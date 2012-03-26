/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.truezip.driver.zip;

import de.truezip.driver.zip.io.ZipKeyException;
import de.truezip.kernel.fs.FsController;
import de.truezip.kernel.key.param.AesPbeParameters;
import java.io.IOException;
import javax.annotation.concurrent.ThreadSafe;

/**
 * This file system controller decorates another file system controller in
 * order to manage the authentication key(s) required for accessing its
 * WinZip AES encrypted ZIP file.
 * 
 * @author Christian Schlichtherle
 */
@ThreadSafe
final class ZipController
extends KeyManagerController<ZipDriver> {

    /**
     * Constructs a new ZIP archive controller.
     *
     * @param controller the file system controller to decorate.
     * @param driver the ZIP driver.
     */
    ZipController(FsController<?> controller, ZipDriver driver) {
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
