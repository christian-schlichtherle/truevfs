/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.truevfs.driver.file;

import javax.annotation.CheckForNull;
import javax.annotation.concurrent.Immutable;
import net.truevfs.kernel.spec.FsController;
import net.truevfs.kernel.spec.FsDriver;
import net.truevfs.kernel.spec.FsManager;
import net.truevfs.kernel.spec.FsModel;

/**
 * A file system driver for the FILE scheme.
 * 
 * @author Christian Schlichtherle
 */
@Immutable
public final class FileDriver extends FsDriver {

    @Override
    public FsController<?> newController(
            final FsManager manager,
            final FsModel model,
            final @CheckForNull FsController<?> parent) {
        assert null == parent;
        return new FileController(model);
    }
}
