/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.truezip.driver.zip;

import de.truezip.driver.zip.io.ZipKeyException;
import de.truezip.kernel.FsController;
import de.truezip.kernel.FsModel;
import de.truezip.key.param.AesPbeParameters;
import java.io.IOException;
import javax.annotation.concurrent.ThreadSafe;

/**
 * This file system controller decorates another file system controller in
 * order to manage its AES PBE parameters.
 * 
 * @param  <M> the type of the file system model.
 * @author Christian Schlichtherle
 */
@ThreadSafe
final class ZipKeyController<M extends FsModel>
extends KeyController<M, ZipDriver> {

    /**
     * Constructs a new ZIP archive controller.
     *
     * @param controller the file system controller to decorate.
     * @param driver the ZIP driver.
     */
    ZipKeyController(FsController<? extends M> controller, ZipDriver driver) {
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