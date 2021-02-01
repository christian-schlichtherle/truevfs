/*
 * Copyright © 2005 - 2021 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package global.namespace.truevfs.driver.file;

import global.namespace.truevfs.kernel.api.FsController;
import global.namespace.truevfs.kernel.api.FsDriver;
import global.namespace.truevfs.kernel.api.FsManager;
import global.namespace.truevfs.kernel.api.FsModel;

import java.util.Optional;

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
            final Optional<? extends FsController> parent) {
        assert !parent.isPresent();
        assert !model.getParent().isPresent();
        return new FileController(model);
    }
}
