/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.kernel.se;

import de.schlichtherle.truezip.kernel.NeedsWriteLockException;
import de.truezip.kernel.FsController;
import de.truezip.kernel.FsDecoratingController;
import java.util.concurrent.locks.ReentrantReadWriteLock.ReadLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.WriteLock;
import javax.annotation.concurrent.Immutable;

/**
 * An abstract file system controller which requires an
 * {@link LockModel} so that it can forward its additional method
 * calls to this model for the convenience of the sub-class.
 *
 * @param  <C> The type of the decorated file system controller.
 * @author Christian Schlichtherle
 */
@Immutable
abstract class DecoratingLockModelController<
        C extends FsController<? extends LockModel>>
extends FsDecoratingController<LockModel, C>  {

    DecoratingLockModelController(C controller) {
        super(controller);
    }

    //
    // These methods are an exact copy of LockModelController.*
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
