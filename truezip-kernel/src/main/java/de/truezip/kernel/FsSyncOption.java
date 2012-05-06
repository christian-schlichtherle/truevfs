/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.truezip.kernel;

import de.truezip.kernel.io.InputClosedException;
import de.truezip.kernel.io.OutputClosedException;
import de.truezip.kernel.util.BitField;
import java.io.IOException;
import javax.annotation.concurrent.Immutable;

/**
 * Defines options for (federated) file system synchronization.
 *
 * @see    FsController#sync(BitField)
 * @author Christian Schlichtherle
 */
@Immutable
public enum FsSyncOption {

    /**
     * Suppose there are any open I/O resources (streams, channels etc.) for
     * any file system entries.
     * Then if this option is set, the respective file system controller waits
     * until all <em>other</em> threads have closed their I/O resources before
     * proceeding with the update of the federated file system.
     * I/O resources allocated by the <em>current</em> thread are always
     * ignored.
     * If the current thread gets interrupted while waiting, it will stop
     * waiting and proceed normally as if this option wasn't set.
     * <p>
     * Beware: If an input resource has not been closed because the client
     * application does not always properly close its streams, even on an
     * {@link IOException} (which is a typical bug in many Java applications),
     * then the respective file system controller will not return from the
     * update until the current thread gets interrupted!
     */
    WAIT_CLOSE_IO,

    /**
     * Suppose there are any open I/O resources (streams, channels etc.) for
     * any file system entries.
     * Then if this option is set, the respective file system controller
     * proceeds to update the federated file system anyway and finally throws
     * an {@link FsSyncWarningException} with a
     * {@link FsResourceOpenException} as its cause to indicate that any
     * subsequent operations on these resources will fail with an
     * {@link InputClosedException} or {@link OutputClosedException}
     * respectively because they have been forced to close.
     * <p>
     * If this option is not set however, the federated file system is
     * <em>not</em> updated, but instead
     * an {@link FsSyncException} with a {@link FsResourceOpenException} as
     * its cause is thrown to indicate
     * that the application must close all input resources first.
     */
    FORCE_CLOSE_IO,

    /**
     * If this option is set, all pending changes are aborted.
     * This option is only meaningful immediately before the federated file
     * system itself gets deleted.
     */
    ABORT_CHANGES,

    /**
     * Suppose a controller for a federated file system has selectively cached
     * entry contents.
     * Then if this option is set when the file system gets synchronized,
     * the entry contents get cleared.
     */
    CLEAR_CACHE,
}
