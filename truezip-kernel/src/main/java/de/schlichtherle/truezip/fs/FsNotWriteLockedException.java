/*
 * Copyright (C) 2011 Schlichtherle IT Services
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
 * Indicates that a resource should be write locked by the current thread,
 * but the write lock cannot get acquired for some reason.
 * This exception type is reserved for use within the TrueZIP Kernel in order
 * to catch it and relock the resource.
 * Unless there is a bug, an exception of this type <em>never</em> pops up to
 * a TrueZIP application.
 *
 * @see     FsConcurrentController
 * @author  Christian Schlichtherle
 * @version $Id$
 */
@ThreadSafe
@DefaultAnnotation(NonNull.class)
public final class FsNotWriteLockedException extends FsException {
    private static final long serialVersionUID = 2345952581284762637L;

    FsNotWriteLockedException() {
    }

    FsNotWriteLockedException(FsNotWriteLockedException ex) {
        super(ex);
    }
}
