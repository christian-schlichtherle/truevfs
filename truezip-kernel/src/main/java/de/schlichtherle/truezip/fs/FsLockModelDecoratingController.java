/*
 * Copyright 2004-2012 Schlichtherle IT Services
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package de.schlichtherle.truezip.fs;

import edu.umd.cs.findbugs.annotations.DefaultAnnotation;
import edu.umd.cs.findbugs.annotations.NonNull;
import java.util.concurrent.locks.ReentrantReadWriteLock.ReadLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.WriteLock;
import net.jcip.annotations.Immutable;

/**
 * An abstract file system controller which requires an
 * {@link FsLockModel} so that it can forward its additional method
 * calls to this model for the convenience of the sub-class.
 *
 * @param   <C> The type of the decorated file system controller.
 * @since   TrueZIP 7.3
 * @author  Christian Schlichtherle
 * @version $Id$
 */
@Immutable
@DefaultAnnotation(NonNull.class)
public abstract class FsLockModelDecoratingController<
        C extends FsController<? extends FsLockModel>>
extends FsDecoratingController<FsLockModel, C>  {

    protected static final int WAIT_TIMEOUT = 100;

    /**
     * Constructs a new decorating file system controller.
     * 
     * @param controller the decorated file system controller.
     */
    protected FsLockModelDecoratingController(C controller) {
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

    protected final void checkWriteLockedByCurrentThread()
    throws FsNeedsWriteLockException {
        getModel().checkWriteLockedByCurrentThread();
    }

    protected final void checkNotReadLockedByCurrentThread()
    throws FsNeedsWriteLockException {
        getModel().checkNotReadLockedByCurrentThread();
    }
}
