/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.fs.nio.file;

import de.schlichtherle.truezip.fs.FsController;
import de.schlichtherle.truezip.fs.FsDriver;
import de.schlichtherle.truezip.fs.FsModel;
import de.schlichtherle.truezip.util.JSE7;
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
    public FsController<?> newController(
            final FsModel model,
            final @CheckForNull FsController<?> parent) {
        assert null == parent;
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