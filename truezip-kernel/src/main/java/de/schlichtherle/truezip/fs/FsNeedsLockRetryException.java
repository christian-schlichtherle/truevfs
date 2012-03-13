/*
 * Copyright (C) 2004-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.fs;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;

/**
 * Indicates that all file system locks need to get released before the
 * operation can get retried.
 *
 * @since  TrueZIP 7.5
 * @see    FsSyncController
 * @author Christian Schlichtherle
 */
@Immutable
@SuppressWarnings("serial") // serializing an exception for a temporary event is nonsense!
public final class FsNeedsLockRetryException extends FsControllerException {

    private static final @Nullable FsNeedsLockRetryException
            SINGLETON = TRACEABLE ? null : new FsNeedsLockRetryException();

    static FsNeedsLockRetryException get(FsModel model) {
        return TRACEABLE    ? new FsNeedsLockRetryException(model, null)
                            : SINGLETON;
    }

    private FsNeedsLockRetryException() { }

    private FsNeedsLockRetryException(FsModel model, @CheckForNull Throwable cause) {
        super(model, null, cause);
    }
}
