/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.kernel.fs;

import de.truezip.kernel.fs.*;
import static de.truezip.kernel.fs.option.FsSyncOptions.SYNC;
import java.io.IOException;
import javax.annotation.concurrent.ThreadSafe;

/**
 * Provides a special variant of {@link #sync(FsNeedsSyncException)} which
 * includes its triggering {@link FsNeedsSyncException} in the
 * {@link FsSyncException} which gets thrown if it fails.
 * 
 * @see    FsNeedsSyncException
 * @author Christian Schlichtherle
 */
@ThreadSafe
class FsSyncDecoratingController<
        M extends FsModel,
        C extends FsController<? extends M>>
extends FsDecoratingController<M, C> {

    /**
     * Constructs a new file system sync controller.
     *
     * @param controller the decorated file system controller.
     */
    FsSyncDecoratingController(C controller) {
        super(controller);
    }

    final void
    sync(final FsNeedsSyncException trigger)
    throws IOException {
        final FsSyncWarningException fuse
                = new FsSyncWarningException(getModel(), trigger);
        final FsSyncExceptionBuilder ied = new FsSyncExceptionBuilder();
        try {
            ied.warn(fuse);     // charge fuse
            sync(SYNC, ied);    // charge load
            ied.check();        // pull trigger
            throw new AssertionError("Expected an instance of the " + FsSyncException.class);
        } catch (final FsSyncWarningException damage) {
            if (damage != fuse) // check for dud
                throw damage;
        }
    }
}