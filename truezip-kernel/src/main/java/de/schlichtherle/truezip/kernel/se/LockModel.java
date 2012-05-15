/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.kernel.se;

import de.schlichtherle.truezip.kernel.NeedsWriteLockException;
import de.truezip.kernel.FsDecoratingModel;
import de.truezip.kernel.FsModel;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.ReadLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.WriteLock;
import javax.annotation.concurrent.ThreadSafe;

/**
 * A file system model which supports multiple concurrent reader threads.
 *
 * @see    LockController
 * @see    NeedsWriteLockException
 * @author Christian Schlichtherle
 */
@ThreadSafe
final class LockModel extends FsDecoratingModel<FsModel> {

    /** The lock on which the file system controllers shall synchronize. */
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock(true);

    LockModel(FsModel model) {
        super(model);
    }

    ReadLock readLock() {
        return lock.readLock();
    }

    /**
     * Returns {@code true} if and only if the read lock is held by the
     * current thread.
     * This method should only get used for assert statements, not for lock
     * control!
     * 
     * @return {@code true} if and only if the read lock is held by the
     *         current thread.
     */
    boolean isReadLockedByCurrentThread() {
        return 0 != lock.getReadHoldCount();
    }

    WriteLock writeLock() {
        return lock.writeLock();
    }

    /**
     * Returns {@code true} if and only if the write lock is held by the
     * current thread.
     * This method should only get used for assert statements, not for lock
     * control!
     * 
     * @return {@code true} if and only if the write lock is held by the
     *         current thread.
     * @see    #checkWriteLockedByCurrentThread()
     */
    boolean isWriteLockedByCurrentThread() {
        return lock.isWriteLockedByCurrentThread();
    }

    /**
     * Asserts that the write lock is held by the current thread.
     * Use this method for lock control.
     * 
     * @throws NeedsWriteLockException if the <i>write lock</i> is not
     *         held by the current thread.
     * @see    #isWriteLockedByCurrentThread()
     */
    void checkWriteLockedByCurrentThread()
    throws NeedsWriteLockException {
        if (!isWriteLockedByCurrentThread())
            throw NeedsWriteLockException.get();
    }
}
