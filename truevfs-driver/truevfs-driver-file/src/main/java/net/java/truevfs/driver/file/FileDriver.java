/*
 * Copyright (C) 2005-2013 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.driver.file;

import javax.annotation.CheckForNull;
import javax.annotation.concurrent.Immutable;
import net.java.truevfs.kernel.spec.*;

/**
 * A file system driver for the FILE scheme.
 *
 * @author Christian Schlichtherle
 */
@Immutable
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
