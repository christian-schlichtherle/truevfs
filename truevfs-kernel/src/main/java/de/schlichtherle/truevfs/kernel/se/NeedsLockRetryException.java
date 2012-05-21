/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truevfs.kernel.se;

import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;

/**
 * Indicates that all file system locks need to get released before the
 * operation can get retried.
 *
 * @see    FsSyncController
 * @author Christian Schlichtherle
 */
@Immutable
@SuppressWarnings("serial") // serializing a control flow exception is nonsense!
final class NeedsLockRetryException extends ControlFlowException {

    private static final @Nullable NeedsLockRetryException
            SINGLETON = TRACEABLE ? null : new NeedsLockRetryException();

    static NeedsLockRetryException get() {
        return TRACEABLE ? new NeedsLockRetryException() : SINGLETON;
    }

    private NeedsLockRetryException() { }
}
