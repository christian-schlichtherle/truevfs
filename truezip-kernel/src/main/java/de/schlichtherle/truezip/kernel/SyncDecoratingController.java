/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.kernel;

import static de.truezip.kernel.FsSyncOptions.SYNC;
import de.truezip.kernel.*;
import javax.annotation.concurrent.ThreadSafe;

/**
 * Provides a special variant of {@link #sync(NeedsSyncException)} which
 * includes its triggering {@link NeedsSyncException} in the
 * {@link FsSyncException} which gets thrown if it fails.
 * 
 * @see    NeedsSyncException
 * @author Christian Schlichtherle
 */
@ThreadSafe
class SyncDecoratingController<
        M extends FsModel,
        C extends FsController<? extends M>>
extends FsDecoratingController<M, C> {

    SyncDecoratingController(C controller) { super(controller); }

    /**
     * Syncs this controller.
     * 
     * @param  trigger the triggering exception.
     * @throws FsSyncWarningException if <em>only</em> warning conditions
     *         apply.
     *         This implies that the respective parent file system has been
     *         synchronized with constraints, e.g. if an unclosed archive entry
     *         stream gets forcibly closed.
     * @throws FsSyncException if any error conditions apply.
     */
    final void sync(final NeedsSyncException trigger)
    throws FsSyncWarningException, FsSyncException {
        try {
            sync(SYNC);
        } catch (final FsSyncException ex) {
            ex.addSuppressed(trigger);
            throw ex;
        }
    }
}
