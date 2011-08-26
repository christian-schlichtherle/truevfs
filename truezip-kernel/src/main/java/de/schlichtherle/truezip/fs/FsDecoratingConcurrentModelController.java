/*
 * Copyright 2011 Schlichtherle IT Services
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
import java.util.concurrent.locks.ReentrantReadWriteLock.ReadLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.WriteLock;
import net.jcip.annotations.Immutable;

/**
 * An abstract file system controller which requires an
 * {@link FsConcurrentModel} so that it can forward its additional method
 * calls to this model for the convenience of the sub-class.
 *
 * @param   <C> The type of the decorated file system controller.
 * @since   TrueZIP 7.3
 * @author  Christian Schlichtherle
 * @version $Id$
 */
@Immutable
@DefaultAnnotation(NonNull.class)
public abstract class FsDecoratingConcurrentModelController<
        C extends FsController<? extends FsConcurrentModel>>
extends FsDecoratingController<FsConcurrentModel, C>  {

    /**
     * Constructs a new decorating file system controller.
     * 
     * @param controller the decorated file system controller.
     */
    protected FsDecoratingConcurrentModelController(C controller) {
        super(controller);
    }

    protected ReadLock readLock() {
        return getModel().readLock();
    }

    protected WriteLock writeLock() {
        return getModel().writeLock();
    }

    protected final boolean isWriteLockedByCurrentThread() {
        return getModel().isWriteLockedByCurrentThread();
    }

    protected final void assertWriteLockedByCurrentThread()
    throws FsNotWriteLockedException {
        getModel().assertWriteLockedByCurrentThread();
    }

    protected final void assertNotReadLockedByCurrentThread(
            @CheckForNull FsNotWriteLockedException ex)
    throws FsNotWriteLockedException {
        getModel().assertNotReadLockedByCurrentThread(ex);
    }
}
