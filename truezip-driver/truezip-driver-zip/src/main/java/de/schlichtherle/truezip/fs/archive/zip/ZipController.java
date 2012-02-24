/*
 * Copyright (C) 2004-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.fs.archive.zip;

import de.schlichtherle.truezip.fs.FsController;
import de.schlichtherle.truezip.key.pbe.AesPbeParameters;
import de.schlichtherle.truezip.zip.ZipKeyException;
import java.io.IOException;
import javax.annotation.concurrent.ThreadSafe;

/**
 * This file system controller decorates another file system controller in
 * order to manage the authentication key(s) required for accessing its target
 * encrypted ZIP archive file.
 * 
 * @author Christian Schlichtherle
 */
@ThreadSafe
final class ZipController
extends KeyManagerController<ZipDriver> {

    /**
     * Constructs a new ZIP archive controller.
     *
     * @param controller the non-{@code null} file system controller to
     *        decorate.
     * @param driver the ZIP driver.
     */
    ZipController(
            final FsController<?> controller,
            final ZipDriver driver) {
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
