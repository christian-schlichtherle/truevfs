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
import net.jcip.annotations.Immutable;

/**
 * Thrown by a file system operation to indicate that a file system lock must
 * get released for a small random amount of time before it can get re-acquired
 * in order to retry the operation.
 * This exception is typically thrown if the lock is already acquired, but
 * should get temporarily released in order to prevent a potential dead lock.
 * <p>
 * This exception type is reserved for use within the TrueZIP Kernel in order
 * to catch it and re-lock the file system.
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
@Immutable
@SuppressWarnings("serial") // serializing an exception for a temporary event is nonsense!
@DefaultAnnotation(NonNull.class)
final class FsNeedsLockRetryException extends FsException {
    FsNeedsLockRetryException() {
        super(null);
    }
}
