/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.truevfs.component.zip.driver;

import java.io.IOException;
import javax.annotation.concurrent.Immutable;
import net.truevfs.component.zip.ZipKeyException;
import net.truevfs.kernel.spec.FsController;
import net.truevfs.kernel.spec.FsModel;
import net.truevfs.keymanager.spec.param.AesPbeParameters;

/**
 * This file system controller decorates another file system controller in
 * order to manage its AES PBE parameters.
 * 
 * @param  <M> the type of the file system model.
 * @author Christian Schlichtherle
 */
@Immutable
public final class ZipKeyController<M extends FsModel>
extends AbstractKeyController<M, AbstractZipDriver> {

    public ZipKeyController(
            FsController<? extends M> controller,
            AbstractZipDriver driver) {
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
