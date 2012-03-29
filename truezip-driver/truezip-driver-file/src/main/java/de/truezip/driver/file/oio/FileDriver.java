/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.truezip.driver.file.oio;

import de.truezip.kernel.fs.FsController;
import de.truezip.kernel.fs.FsDriver;
import de.truezip.kernel.fs.FsManager;
import de.truezip.kernel.fs.FsModel;
import javax.annotation.CheckForNull;
import javax.annotation.concurrent.Immutable;

/**
 * A file system driver for the FILE scheme.
 * 
 * @author Christian Schlichtherle
 */
@Immutable
public final class FileDriver extends FsDriver {

    @Override
    public FsController<?>
    newController(  final FsManager manager,
                    final FsModel model,
                    final @CheckForNull FsController<?> parent) {
        assert null == parent;
        return new FileController(model);
    }
}
