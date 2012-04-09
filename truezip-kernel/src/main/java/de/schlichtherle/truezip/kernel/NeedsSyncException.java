/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.kernel;

import de.truezip.kernel.FsControlFlowIOException;
import de.truezip.kernel.FsEntryName;
import de.truezip.kernel.cio.Entry.Access;
import de.truezip.kernel.FsModel;
import de.truezip.kernel.util.BitField;
import javax.annotation.CheckForNull;
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
final class NeedsSyncException extends FsControlFlowIOException {

    private static final @Nullable NeedsSyncException
            SINGLETON = TRACEABLE ? null : new NeedsSyncException();

    static NeedsSyncException get( final FsModel model,
                                            final FsEntryName name,
                                            final @CheckForNull Access access) {
        return TRACEABLE    ? new NeedsSyncException(model,
                                (null == access ? "touch" : access.toString())
                                    + ' ' + name,
                                null)
                            : SINGLETON;
    }

    static NeedsSyncException get( final FsModel model,
                                            final String name,
                                            final Throwable cause) {
        return TRACEABLE    ? new NeedsSyncException(model, name, cause)
                            : SINGLETON;
    }

    private NeedsSyncException() { }

    private NeedsSyncException(   final FsModel model,
                                    final String message,
                                    final @CheckForNull Throwable cause) {
        super(model, message, cause);
    }
}