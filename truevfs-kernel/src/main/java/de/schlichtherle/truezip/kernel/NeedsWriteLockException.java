/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.kernel;

import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;

/**
 * Indicates that the file system needs to get write locked before the
 * operation can get retried.
 *
 * @see    FsLockController
 * @author Christian Schlichtherle
 */
@Immutable
@SuppressWarnings("serial") // serializing an exception for a temporary event is nonsense!
public final class NeedsWriteLockException extends ControlFlowException {

    private static final @Nullable NeedsWriteLockException
            SINGLETON = TRACEABLE ? null : new NeedsWriteLockException();

    public static NeedsWriteLockException get() {
        return TRACEABLE ? new NeedsWriteLockException() : SINGLETON;
    }

    private NeedsWriteLockException() { }
}
