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
 * {@link LockModel} so that it can forward calls to its additional
 * protected methods to this model for the convenience of sub-classes.
 *
 * @see    LockModel
 * @author Christian Schlichtherle
 */
@Immutable
abstract class LockModelController
extends FsModelController<LockModel>  {

    LockModelController(LockModel model) {
        super(model);
    }

    //
    // These methods are an exact copy of DecoratingLockModelController.*
    //

    ReadLock readLock() {
        return getModel().readLock();
    }

    final boolean isReadLockedByCurrentThread() {
        return getModel().isReadLockedByCurrentThread();
    }

    WriteLock writeLock() {
        return getModel().writeLock();
    }

    final boolean isWriteLockedByCurrentThread() {
        return getModel().isWriteLockedByCurrentThread();
    }

    final void checkWriteLockedByCurrentThread()
    throws NeedsWriteLockException {
        getModel().checkWriteLockedByCurrentThread();
    }
}