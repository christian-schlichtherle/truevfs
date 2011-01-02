/*
 * Copyright (C) 2010 Schlichtherle IT Services
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
package de.schlichtherle.truezip.io.filesystem.concurrent;

import de.schlichtherle.truezip.io.filesystem.FileSystemModel;
import de.schlichtherle.truezip.io.filesystem.MountPoint;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.NonNull;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.ReadLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.WriteLock;
import net.jcip.annotations.ThreadSafe;

/**
 * Supports multiple concurrent reader threads.
 *
 * @see     ConcurrentFileSystemController
 * @author  Christian Schlichtherle
 * @version $Id$
 */
@ThreadSafe
public class ConcurrentFileSystemModel extends FileSystemModel {
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    public ConcurrentFileSystemModel(   @NonNull MountPoint mountPoint,
                                        @CheckForNull FileSystemModel parent) {
        super(mountPoint, parent);
    }

    public final ReadLock readLock() {
        return lock.readLock();
    }

    public final WriteLock writeLock() {
        return lock.writeLock();
    }

    /**
     * @throws NotWriteLockedException if the <i>write lock</i> is not
     *         held by the current thread.
     */
    public final void assertWriteLockedByCurrentThread()
    throws NotWriteLockedException {
        if (!lock.isWriteLockedByCurrentThread())
            throw new NotWriteLockedException(this);
    }

    /**
     * @throws NotWriteLockedException if the <i>read lock</i> is
     *         held by the current thread.
     */
    public final void assertNotReadLockedByCurrentThread(NotWriteLockedException ex)
    throws NotWriteLockedException {
        if (0 < lock.getReadHoldCount())
            throw new NotWriteLockedException(this, ex);
    }
}
