/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.truezip.kernel.fs;

import de.truezip.kernel.fs.addr.FsEntryName;
import de.truezip.kernel.cio.Entry.Access;
import de.truezip.kernel.util.BitField;
import javax.annotation.CheckForNull;
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
final class FsNeedsSyncException extends FsControllerException {

    private static final @Nullable FsNeedsSyncException
            SINGLETON = TRACEABLE ? null : new FsNeedsSyncException();

    static FsNeedsSyncException get( final FsModel model,
                                            final FsEntryName name,
                                            final @CheckForNull Access access) {
        return TRACEABLE    ? new FsNeedsSyncException(model,
                                (null == access ? "touch" : access.toString())
                                    + ' ' + name,
                                null)
                            : SINGLETON;
    }

    static FsNeedsSyncException get( final FsModel model,
                                            final String name,
                                            final Throwable cause) {
        return TRACEABLE    ? new FsNeedsSyncException(model, name, cause)
                            : SINGLETON;
    }

    private FsNeedsSyncException() { }

    private FsNeedsSyncException(   final FsModel model,
                                    final String message,
                                    final @CheckForNull Throwable cause) {
        super(model, message, cause);
    }
}
