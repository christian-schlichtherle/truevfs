/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.kernel;

import de.truezip.kernel.FsControlFlowIOException;
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
final class NeedsWriteLockException extends FsControlFlowIOException {

    private static final @Nullable NeedsWriteLockException
            SINGLETON = TRACEABLE ? null : new NeedsWriteLockException();

    static NeedsWriteLockException get(FsModel model) {
        return TRACEABLE ? new NeedsWriteLockException(model) : SINGLETON;
    }

    private NeedsWriteLockException() { }

    private NeedsWriteLockException(FsModel model) {
        super(model, null, null);
    }
}
