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
import de.schlichtherle.truezip.util.concurrent.lock.ReentrantLock;
import de.schlichtherle.truezip.util.concurrent.lock.ReentrantReadWriteLock;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.NonNull;
import net.jcip.annotations.ThreadSafe;

/**
 * Supports multiple concurrent reader threads.
 *
 * @author Christian Schlichtherle
 * @version $Id$
 */
@ThreadSafe
public class ConcurrentFileSystemModel extends FileSystemModel {
    private final ReentrantLock readLock;
    private final ReentrantLock writeLock;

    public ConcurrentFileSystemModel(   @NonNull final MountPoint mountPoint,
                                        @CheckForNull final FileSystemModel parent) {
        super(mountPoint, parent);
        final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
        readLock = lock.readLock();
        writeLock = lock.writeLock();
    }

    public final ReentrantLock readLock() {
        return readLock;
    }

    public final ReentrantLock writeLock() {
        return writeLock;
    }

    /**
     * @throws NotWriteLockedException if the <i>write lock</i> is not
     *         held by the current thread.
     */
    public final void assertWriteLockedByCurrentThread()
    throws NotWriteLockedException {
        if (!writeLock().isHeldByCurrentThread())
            throw new NotWriteLockedException(this);
    }

    /**
     * @throws NotWriteLockedException if the <i>read lock</i> is
     *         held by the current thread.
     */
    public final void assertNotReadLockedByCurrentThread(NotWriteLockedException ex)
    throws NotWriteLockedException {
        if (readLock().isHeldByCurrentThread())
            throw new NotWriteLockedException(this, ex);
    }
}
