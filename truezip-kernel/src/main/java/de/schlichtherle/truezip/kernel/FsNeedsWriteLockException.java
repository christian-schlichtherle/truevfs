/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.kernel;

import de.schlichtherle.truezip.kernel.FsControllerException;
import de.truezip.kernel.FsModel;
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
final class FsNeedsWriteLockException extends FsControllerException {

    private static final @Nullable FsNeedsWriteLockException
            SINGLETON = TRACEABLE ? null : new FsNeedsWriteLockException();

    static FsNeedsWriteLockException get(FsModel model) {
        return TRACEABLE ? new FsNeedsWriteLockException(model) : SINGLETON;
    }

    private FsNeedsWriteLockException() { }

    private FsNeedsWriteLockException(FsModel model) {
        super(model, null, null);
    }
}
