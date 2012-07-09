/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.fs.archive.zip.raes;

import de.schlichtherle.truezip.crypto.raes.RaesKeyException;
import de.schlichtherle.truezip.crypto.raes.param.AesCipherParameters;
import de.schlichtherle.truezip.fs.FsController;
import de.schlichtherle.truezip.fs.archive.zip.KeyController;
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
final class ZipRaesKeyController extends KeyController<ZipRaesDriver> {

    ZipRaesKeyController(FsController<?> controller, ZipRaesDriver driver) {
        super(controller, driver);
    }

    @Override
    protected Class<?> getKeyType() {
        return AesCipherParameters.class;
    }

    @Override
    protected Class<? extends IOException> getKeyExceptionType() {
        return RaesKeyException.class;
    }
}
