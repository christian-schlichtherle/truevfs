/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.kernel;

import de.truezip.kernel.*;
import static de.truezip.kernel.FsSyncOptions.SYNC;
import java.io.IOException;
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

    /**
     * Constructs a new file system sync controller.
     *
     * @param controller the decorated file system controller.
     */
    SyncDecoratingController(C controller) {
        super(controller);
    }

    final void
    sync(final NeedsSyncException trigger)
    throws IOException {
        try {
            sync(SYNC);
        } catch (final IOException ex) {
            ex.addSuppressed(trigger);
            throw ex;
        }
    }
}
