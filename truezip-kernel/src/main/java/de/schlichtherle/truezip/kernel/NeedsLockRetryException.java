/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.kernel;

import de.truezip.kernel.FsModel;
import javax.annotation.CheckForNull;
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
@SuppressWarnings("serial") // serializing an exception for a temporary event is nonsense!
final class NeedsLockRetryException extends FsControlFlowIOException {

    private static final @Nullable NeedsLockRetryException
            SINGLETON = TRACEABLE ? null : new NeedsLockRetryException();

    static NeedsLockRetryException get(FsModel model) {
        return TRACEABLE    ? new NeedsLockRetryException(model, null)
                            : SINGLETON;
    }

    private NeedsLockRetryException() { }

    private NeedsLockRetryException(FsModel model, @CheckForNull Throwable cause) {
        super(model, null, cause);
    }
}