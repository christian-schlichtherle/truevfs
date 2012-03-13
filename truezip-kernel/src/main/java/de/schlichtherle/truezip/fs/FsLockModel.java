/*
 * Copyright (C) 2004-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.fs;

import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.ReadLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.WriteLock;
import javax.annotation.concurrent.ThreadSafe;

/**
 * A file system model which supports multiple concurrent reader threads.
 *
 * @see    FsLockController
 * @author Christian Schlichtherle
 */
@ThreadSafe
public final class FsLockModel extends FsDecoratingModel<FsModel> {

    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    public FsLockModel(FsModel model) {
        super(model);
    }

    public ReadLock readLock() {
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

    public WriteLock writeLock() {
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
    public boolean isWriteLockedByCurrentThread() {
        return lock.isWriteLockedByCurrentThread();
    }

    /**
     * Asserts that the write lock is held by the current thread.
     * Use this method for lock control.
     * 
     * @throws FsNeedsWriteLockException if the <i>write lock</i> is not
     *         held by the current thread.
     * @see    #isWriteLockedByCurrentThread()
     */
    public void checkWriteLockedByCurrentThread()
    throws FsNeedsWriteLockException {
        if (!isWriteLockedByCurrentThread())
            throw FsNeedsWriteLockException.get(this);
    }
}
