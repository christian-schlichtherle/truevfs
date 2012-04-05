/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.kernel;

import de.truezip.kernel.FsModelController;
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
extends FsModelController<FsLockModel>  {

    /**
     * Constructs a new file system controller for the given file system lock
     * model.
     * 
     * @param model the file system lock model.
     */
    FsLockModelController(FsLockModel model) {
        super(model);
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