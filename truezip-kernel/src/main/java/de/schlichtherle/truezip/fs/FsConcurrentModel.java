/*
 * Copyright (C) 2011 Schlichtherle IT Services
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.schlichtherle.truezip.fs;

import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.DefaultAnnotation;
import edu.umd.cs.findbugs.annotations.NonNull;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.ReadLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.WriteLock;
import net.jcip.annotations.ThreadSafe;

/**
 * A file system model which supports multiple concurrent reader threads.
 *
 * @see     FsConcurrentController
 * @author  Christian Schlichtherle
 * @version $Id$
 */
@ThreadSafe
@DefaultAnnotation(NonNull.class)
public final class FsConcurrentModel extends FsDecoratingModel<FsModel> {

    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    public FsConcurrentModel(FsModel model) {
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
     * @see    #assertWriteLockedByCurrentThread()
     */
    public boolean isWriteLockedByCurrentThread() {
        return lock.isWriteLockedByCurrentThread();
    }

    /**
     * Asserts that the write lock is held by the current thread.
     * Use this method for lock control.
     * 
     * @throws FsNotWriteLockedException if the <i>write lock</i> is not
     *         held by the current thread.
     * @see    #isWriteLockedByCurrentThread()
     */
    public void assertWriteLockedByCurrentThread()
    throws FsNotWriteLockedException {
        if (!lock.isWriteLockedByCurrentThread())
            throw new FsNotWriteLockedException();
    }

    /**
     * Asserts that the read lock is <em>not</em> held by the current thread,
     * so that the caller can safely acquire the write lock without dead
     * locking.
     * Use this method for lock control.
     * 
     * @param  ex the caught exception.
     * @throws FsNotWriteLockedException if the <i>read lock</i> is
     *         held by the current thread.
     */
    void assertNotReadLockedByCurrentThread(
            @CheckForNull FsNotWriteLockedException ex)
    throws FsNotWriteLockedException {
        if (0 < lock.getReadHoldCount())
            throw new FsNotWriteLockedException(ex);
    }

    /**
     * Returns a string representation of this object for debugging and logging
     * purposes.
     */
    @Override
    public String toString() {
        return new StringBuilder()
                .append(getClass().getName())
                .append("[delegate=")
                .append(delegate)
                .append("]")
                .toString();
    }
}
