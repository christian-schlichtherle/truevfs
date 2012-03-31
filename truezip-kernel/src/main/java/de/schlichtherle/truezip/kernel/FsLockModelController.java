/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.kernel;

import de.truezip.kernel.FsController;
import java.util.concurrent.locks.ReentrantReadWriteLock.ReadLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.WriteLock;
import javax.annotation.concurrent.Immutable;

/**
 * An abstract file system controller which requires an
 * {@link FsLockModel} so that it can forward calls to its additional
 * protected methods to this model for the convenience of sub-classes.
 *
 * @see    FsLockModel
 * @author Christian Schlichtherle
 */
@Immutable
abstract class FsLockModelController
extends FsController<FsLockModel>  {

    /**
     * Constructs a new file system controller for the given
     * concurrent file system model.
     * 
     * @param model the concurrent file system model.
     */
    FsLockModelController(FsLockModel model) {
        super(model);
    }

    final boolean isTouched() {
        return getModel().isTouched();
    }

    final void setTouched(boolean touched) {
        getModel().setTouched(touched);
    }

    ReadLock readLock() {
        return getModel().readLock();
    }

    WriteLock writeLock() {
        return getModel().writeLock();
    }

    final boolean isWriteLockedByCurrentThread() {
        return getModel().isWriteLockedByCurrentThread();
    }

    final void checkWriteLockedByCurrentThread()
    throws FsNeedsWriteLockException {
        getModel().checkWriteLockedByCurrentThread();
    }
}