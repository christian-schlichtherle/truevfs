/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.fs;

import de.schlichtherle.truezip.util.BitField;
import de.schlichtherle.truezip.util.ControlFlowException;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;

/**
 * Indicates that a file system controller needs to get
 * {@linkplain FsController#sync(BitField) synced} before the operation can
 * get retried.
 *
 * @since  TrueZIP 7.3
 * @see    FsSyncController
 * @author Christian Schlichtherle
 */
@Immutable
@SuppressWarnings("serial") // serializing an exception for a temporary event is nonsense!
final class FsNeedsSyncException extends ControlFlowException {

    private static final @Nullable FsNeedsSyncException
            INSTANCE = new FsNeedsSyncException();

    private FsNeedsSyncException() { }

    public static FsNeedsSyncException get() {
        return isTraceable() ? new FsNeedsSyncException() : INSTANCE;
    }
}
