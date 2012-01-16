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
import net.jcip.annotations.ThreadSafe;

/**
 * Thrown by a file system operation to indicate that the file system needs to
 * get write locked by the current thread before the operation can get retried.
 * This exception is typically thrown if the read lock is already acquired,
 * so that updating the lock would just dead lock the current thread.
 * <p>
 * This exception type is reserved for use within the TrueZIP Kernel in order
 * to catch it and relock the resource.
 * Unless there is a bug, an exception of this type <em>never</em> pops up to
 * a TrueZIP application.
 * <p>
 * ONLY THE TRUEZIP KERNEL SHOULD THROW AN EXCEPTION OF THIS TYPE!
 * DO NOT CREATE OR THROW AN EXCEPTION OF THIS TYPE (INCLUDING SUB-CLASSES)
 * ANYWHERE ELSE!
 *
 * @see     FsLockController
 * @author  Christian Schlichtherle
 * @version $Id$
 */
@ThreadSafe
@DefaultAnnotation(NonNull.class)
public final class FsNeedsWriteLockException extends FsException {
    private static final long serialVersionUID = 2345952581284762637L;

    FsNeedsWriteLockException() {
    }

    FsNeedsWriteLockException(FsNeedsWriteLockException ex) {
        super(ex);
    }
}
