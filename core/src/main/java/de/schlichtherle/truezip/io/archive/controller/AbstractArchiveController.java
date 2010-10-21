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
package de.schlichtherle.truezip.io.archive.controller;

import de.schlichtherle.truezip.io.archive.entry.ArchiveEntry;

/**
 * @author Christian Schlichtherle
 * @version $Id$
 */
abstract class AbstractArchiveController<AE extends ArchiveEntry>
implements     ArchiveController        <AE> {

    @Override
    public final String toString() {
        return "archiveController:" + getModel().getMountPoint().toString();
    }

    /**
     * @throws NotWriteLockedException if the <i>write lock</i> is not
     *         held by the current thread.
     */
    final void assertWriteLockedByCurrentThread()
    throws NotWriteLockedException {
        final ArchiveModel model = getModel();
        if (!model.writeLock().isHeldByCurrentThread())
            throw new NotWriteLockedException(model);
    }

    /**
     * @throws NotWriteLockedException if the <i>read lock</i> is
     *         held by the current thread.
     */
    final void assertNotReadLockedByCurrentThread(NotWriteLockedException ex)
    throws NotWriteLockedException {
        final ArchiveModel model = getModel();
        if (model.readLock().isHeldByCurrentThread())
            throw new NotWriteLockedException(model, ex);
    }
}
