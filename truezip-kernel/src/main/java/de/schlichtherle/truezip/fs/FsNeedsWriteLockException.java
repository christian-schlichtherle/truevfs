/*
 * Copyright (C) 2004-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.fs;

import javax.annotation.concurrent.Immutable;

/**
 * Indicates that the file system needs to get write locked before the
 * operation can proceed.
 *
 * @see    FsLockController
 * @author Christian Schlichtherle
 */
@Immutable
@SuppressWarnings("serial") // serializing an exception for a temporary event is nonsense!
public final class FsNeedsWriteLockException extends FsControllerException {
    static FsNeedsWriteLockException get() {
        return TRACE ? new FsNeedsWriteLockException() : SINGLETON;
    }

    private static final FsNeedsWriteLockException
            SINGLETON = TRACE ? null : new FsNeedsWriteLockException();
}
