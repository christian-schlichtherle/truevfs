/*
 * Copyright 2004-2012 Schlichtherle IT Services
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package de.schlichtherle.truezip.fs;

import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.ReadLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.WriteLock;
import javax.annotation.concurrent.ThreadSafe;

/**
 * A file system model which supports multiple concurrent reader threads.
 *
 * @see     FsLockController
 * @author  Christian Schlichtherle
 * @version $Id$
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
        if (!lock.isWriteLockedByCurrentThread())
            throw FsNeedsWriteLockException.SINGLETON;
    }

    /**
     * Asserts that the read lock is <em>not</em> held by the current thread,
     * so that the caller can safely acquire the write lock without dead
     * locking.
     * Use this method for lock control.
     * 
     * @throws FsNeedsWriteLockException if the <i>read lock</i> is
     *         held by the current thread.
     */
    void checkNotReadLockedByCurrentThread()
    throws FsNeedsWriteLockException {
        if (0 < lock.getReadHoldCount())
            throw FsNeedsWriteLockException.SINGLETON;
    }
}
