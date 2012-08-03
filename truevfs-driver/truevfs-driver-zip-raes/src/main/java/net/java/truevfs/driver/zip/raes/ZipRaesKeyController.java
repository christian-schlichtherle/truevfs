/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.driver.zip.raes;

import java.io.IOException;
import javax.annotation.concurrent.Immutable;
import net.java.truevfs.component.zip.driver.AbstractKeyController;
import net.java.truevfs.driver.zip.raes.crypto.RaesKeyException;
import net.java.truevfs.kernel.spec.FsController;
import net.java.truevfs.kernel.spec.FsModel;
import net.java.truevfs.keymanager.spec.param.AesPbeParameters;

/**
 * This file system controller decorates another file system controller in
 * order to manage the authentication key required for accessing its
 * RAES encrypted ZIP file (ZIP.RAES).
 * 
 * @param  <M> the type of the file system model.
 * @author Christian Schlichtherle
 */
@Immutable
final class ZipRaesKeyController<M extends FsModel>
extends AbstractKeyController<M, ZipRaesDriver> {

    ZipRaesKeyController(FsController<M> controller, ZipRaesDriver driver) {
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
