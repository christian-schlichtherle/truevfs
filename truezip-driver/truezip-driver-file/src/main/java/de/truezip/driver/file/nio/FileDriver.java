/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.truezip.driver.file.nio;

import de.truezip.kernel.fs.FsController;
import de.truezip.kernel.fs.FsDriver;
import de.truezip.kernel.fs.FsModel;
import de.truezip.kernel.util.JSE7;
import javax.annotation.CheckForNull;
import javax.annotation.concurrent.Immutable;

/**
 * A file system driver for the FILE scheme.
 * 
 * @since  TrueZIP 7.2
 * @author Christian Schlichtherle
 */
@Immutable
public final class FileDriver extends FsDriver {

    @Override
    public FsController<?>
    newController(FsModel model, @CheckForNull FsController<?> parent) {
        assert null == model.getParent()
                ? null == parent
                : model.getParent().equals(parent.getModel());
        if (null != parent)
            throw new IllegalArgumentException();
        return new FileController(model);
    }

    /**
     * {@inheritDoc}
     * 
     * @return {@code 100} or {@link Integer#MIN_VALUE}, depending on the
     *         availability of the NIO.2 API.
     */
    @Override
    public int getPriority() {
        return JSE7.AVAILABLE ? 100 : Integer.MIN_VALUE;
    }
}
