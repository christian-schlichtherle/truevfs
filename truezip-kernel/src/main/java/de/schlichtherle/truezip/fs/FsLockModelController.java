/*
 * Copyright (C) 2004-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.fs;

import java.util.concurrent.locks.ReentrantReadWriteLock.ReadLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.WriteLock;
import javax.annotation.concurrent.Immutable;

/**
 * An abstract file system controller which requires an
 * {@link FsLockModel} so that it can forward calls to its additional
 * protected methods to this model for the convenience of sub-classes.
 *
 * @see    FsLockModel
 * @since  TrueZIP 7.3
 * @author Christian Schlichtherle
 */
@Immutable
public abstract class FsLockModelController
extends FsModelController<FsLockModel>  {

    /**
     * Constructs a new file system controller for the given
     * concurrent file system model.
     * 
     * @param model the concurrent file system model.
     */
    protected FsLockModelController(FsLockModel model) {
        super(model);
    }

    protected ReadLock readLock() {
        return getModel().readLock();
    }

    protected WriteLock writeLock() {
        return getModel().writeLock();
    }

    protected final void checkWriteLockedByCurrentThread()
    throws FsNeedsWriteLockException {
        getModel().checkWriteLockedByCurrentThread();
    }
}
