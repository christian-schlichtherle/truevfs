/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.kernel;

import de.truezip.kernel.FsController;
import de.truezip.kernel.util.BitField;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;

/**
 * Indicates that a file system controller needs to get
 * {@linkplain FsController#sync(BitField) synced} before the operation can
 * get retried.
 *
 * @see    FsSyncController
 * @author Christian Schlichtherle
 */
@Immutable
@SuppressWarnings("serial") // serializing an exception for a temporary event is nonsense!
final class NeedsSyncException extends ControlFlowException {

    private static final @Nullable NeedsSyncException
            SINGLETON = TRACEABLE ? null : new NeedsSyncException();

    static NeedsSyncException get() {
        return TRACEABLE ? new NeedsSyncException() : SINGLETON;
    }

    private NeedsSyncException() { }
}
