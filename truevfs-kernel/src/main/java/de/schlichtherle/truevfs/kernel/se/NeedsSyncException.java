/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truevfs.kernel.se;

import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import net.truevfs.kernel.FsController;
import net.truevfs.kernel.util.BitField;

/**
 * Indicates that a file system controller needs to get
 * {@linkplain FsController#sync(BitField) synced} before the operation can
 * get retried.
 *
 * @see    FsSyncController
 * @author Christian Schlichtherle
 */
@Immutable
@SuppressWarnings("serial") // serializing a control flow exception is nonsense!
final class NeedsSyncException extends ControlFlowException {

    private static final @Nullable NeedsSyncException
            SINGLETON = TRACEABLE ? null : new NeedsSyncException();

    static NeedsSyncException get() {
        return TRACEABLE ? new NeedsSyncException() : SINGLETON;
    }

    private NeedsSyncException() { }
}
