/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.kernel.spec;

import javax.annotation.concurrent.ThreadSafe;
import net.java.truecommons.shed.BitField;

/**
 * A visitor which performs a {@link FsController#sync} operation on
 * each file system controller which has been accepted by the {@link #filter}.
 * <p>
 * Subclasses should be thread-safe.
 *
 * @author Christian Schlichtherle
 */
@ThreadSafe
public abstract class FsControllerSyncVisitor
implements FsControllerVisitor<FsSyncException> {

    /**
     * Returns a new {@link FsSyncExceptionBuilder}.
     * 
     * @return A new {@link FsSyncExceptionBuilder}.
     */
    @Override
    public final FsSyncExceptionBuilder builder() {
        return new FsSyncExceptionBuilder();
    }

    /**
     * Obtains the {@linkplain #options options} for the given file system
     * controller and calls {@link FsController#sync} on it.
     * 
     * @param  controller the file system controller to {@code sync()}.
     * @throws FsSyncException if {@code sync()}ing the file system failed for
     *         some reason.
     */
    @Override
    public void visit(FsController controller) throws FsSyncException {
        controller.sync(options(controller));
    }

    /**
     * Returns the options to use for {@linkplain FsController#sync sync()}ing
     * the given file system controller.
     * 
     * @param  controller the file system controller to {@code sync()}.
     * @return The options to use for {@linkplain FsController#sync sync()}ing
     *         the given file system controller.
     */
    public abstract BitField<FsSyncOption> options(FsController controller);
}
