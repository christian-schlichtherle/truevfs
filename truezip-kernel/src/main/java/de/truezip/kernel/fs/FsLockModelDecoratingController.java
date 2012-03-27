/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.truezip.kernel.fs;

import java.util.concurrent.locks.ReentrantReadWriteLock.ReadLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.WriteLock;
import javax.annotation.concurrent.Immutable;

/**
 * An abstract file system controller which requires an
 * {@link FsLockModel} so that it can forward its additional method
 * calls to this model for the convenience of the sub-class.
 *
 * @param  <C> The type of the decorated file system controller.
 * @author Christian Schlichtherle
 */
@Immutable
abstract class FsLockModelDecoratingController<
        C extends FsController<? extends FsLockModel>>
extends FsDecoratingController<FsLockModel, C>  {

    static final int WAIT_TIMEOUT_MILLIS = 100;

    /**
     * Constructs a new decorating file system controller.
     * 
     * @param controller the decorated file system controller.
     */
    FsLockModelDecoratingController(C controller) {
        super(controller);
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