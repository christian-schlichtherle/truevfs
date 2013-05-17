/*
 * Copyright (C) 2005-2013 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.comp.zipdriver;

import java.io.IOException;
import javax.annotation.concurrent.Immutable;
import net.java.truevfs.comp.zip.ZipKeyException;
import net.java.truevfs.kernel.spec.FsController;
import net.java.truecommons.key.spec.common.AesPbeParameters;

/**
 * This file system controller decorates another file system controller in
 * order to manage its AES PBE parameters.
 *
 * @author Christian Schlichtherle
 */
@Immutable
public final class ZipKeyController
extends AbstractKeyController<AbstractZipDriver<?>> {

    public ZipKeyController(
            FsController controller,
            AbstractZipDriver<?> driver) {
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
