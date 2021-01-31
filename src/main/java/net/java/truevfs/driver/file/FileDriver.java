/*
 * Copyright © 2005 - 2021 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.driver.file;

import net.java.truevfs.kernel.spec.FsController;
import net.java.truevfs.kernel.spec.FsDriver;
import net.java.truevfs.kernel.spec.FsManager;
import net.java.truevfs.kernel.spec.FsModel;

import javax.annotation.CheckForNull;

/**
 * A file system driver for the FILE scheme.
 *
 * @author Christian Schlichtherle
 */
public final class FileDriver extends FsDriver {

    @Override
    public FsController newController(
            final FsManager manager,
            final FsModel model,
            final @CheckForNull FsController parent) {
        assert null == parent;
        assert null == model.getParent();
        return new FileController(model);
    }
}
