/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.truevfs.kernel.spec;

import javax.annotation.CheckForNull;
import javax.annotation.concurrent.ThreadSafe;

/**
 * Defines the common properties of a file system.
 * <p>
 * Sub-classes must be thread-safe, too.
 *
 * @see    FsController
 * @author Christian Schlichtherle
 */
@ThreadSafe
public class FsDefaultModel extends FsAbstractModel {

    public FsDefaultModel(
            FsMountPoint mountPoint,
            @CheckForNull FsModel parent) {
        super(mountPoint, parent);
    }

    /**
     * {@inheritDoc}
     * <p>
     * The implementation in the class {@link FsModel} always
     * returns {@code false}.
     */
    @Override
    public boolean isTouched() {
        return false;
    }

    /**
     * {@inheritDoc}
     * <p>
     * The implementation in the class {@link FsModel} always
     * throws an {@link UnsupportedOperationException}.
     *
     * @throws UnsupportedOperationException the implementation in the class
     *         {@link FsModel} always throws an
     *         {@link UnsupportedOperationException}.
     */
    @Override
    public void setTouched(boolean touched) {
        throw new UnsupportedOperationException();
    }
}
