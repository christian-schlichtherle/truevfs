/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.truevfs.driver.zip.raes;

import net.truevfs.driver.zip.KeyController;
import net.truevfs.driver.zip.raes.crypto.RaesKeyException;
import net.truevfs.kernel.FsController;
import net.truevfs.kernel.FsModel;
import net.truevfs.key.param.AesPbeParameters;
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
final class ZipRaesKeyController<M extends FsModel>
extends KeyController<M, ZipRaesDriver> {

    /**
     * Constructs a new ZIP.RAES archive controller.
     *
     * @param controller the file system controller to decorate.
     * @param driver the ZIP.RAES driver.
     */
    ZipRaesKeyController(FsController<? extends M> controller, ZipRaesDriver driver) {
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
