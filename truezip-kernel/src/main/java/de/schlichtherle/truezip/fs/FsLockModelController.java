/*
 * Copyright 2004-2012 Schlichtherle IT Services
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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
 * {@link FsLockModel} so that it can forward calls to its additional
 * protected methods to this model for the convenience of sub-classes.
 *
 * @see     FsLockModel
 * @since   TrueZIP 7.3
 * @author  Christian Schlichtherle
 * @version $Id$
 */
@Immutable
@DefaultAnnotation(NonNull.class)
public abstract class FsLockModelController
extends FsModelController<FsLockModel>  {

    /**
     * Constructs a new file system controller for the given
     * concurrent file system model.
     * 
     * @param model the concurrent file system model.
     */
    protected FsLockModelController(FsLockModel model) {
        super(model);
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
    throws FsNeedsWriteLockException {
        getModel().assertWriteLockedByCurrentThread();
    }

    protected final void assertNotReadLockedByCurrentThread(
            @CheckForNull FsNeedsWriteLockException ex)
    throws FsNeedsWriteLockException {
        getModel().assertNotReadLockedByCurrentThread(ex);
    }
}
