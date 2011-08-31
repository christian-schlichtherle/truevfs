/*
 * Copyright 2011 Schlichtherle IT Services
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
 * {@link FsConcurrentModel} so that it can forward calls to its additional
 * protected methods to this model for the convenience of sub-classes.
 *
 * @see     FsConcurrentModel
 * @since   TrueZIP 7.3
 * @author  Christian Schlichtherle
 * @version $Id$
 */
@Immutable
@DefaultAnnotation(NonNull.class)
public abstract class FsConcurrentModelController
extends FsModelController<FsConcurrentModel>  {

    /**
     * Constructs a new file system controller for the given
     * concurrent file system model.
     * 
     * @param model the concurrent file system model.
     */
    protected FsConcurrentModelController(FsConcurrentModel model) {
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
    throws FsNotWriteLockedException {
        getModel().assertWriteLockedByCurrentThread();
    }

    protected final void assertNotReadLockedByCurrentThread(
            @CheckForNull FsNotWriteLockedException ex)
    throws FsNotWriteLockedException {
        getModel().assertNotReadLockedByCurrentThread(ex);
    }
}
