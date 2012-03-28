/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.truezip.driver.zip.raes;

import de.truezip.driver.zip.KeyManagerController;
import de.truezip.driver.zip.raes.crypto.RaesKeyException;
import de.truezip.kernel.fs.FsController;
import de.truezip.kernel.fs.FsModel;
import de.truezip.key.param.AesPbeParameters;
import java.io.IOException;
import javax.annotation.concurrent.ThreadSafe;

/**
 * This file system controller decorates another file system controller in
 * order to manage the authentication key required for accessing its
 * RAES encrypted ZIP file (ZIP.RAES).
 * 
 * @author Christian Schlichtherle
 */
@ThreadSafe
final class ZipRaesController<M extends FsModel>
extends KeyManagerController<M, ZipRaesDriver> {

    /**
     * Constructs a new ZIP.RAES archive controller.
     *
     * @param controller the file system controller to decorate.
     * @param driver the ZIP.RAES driver.
     */
    ZipRaesController(FsController<? extends M> controller, ZipRaesDriver driver) {
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