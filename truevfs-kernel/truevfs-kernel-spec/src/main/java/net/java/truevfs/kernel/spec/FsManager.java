/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.kernel.spec;

import java.io.Closeable;
import java.io.IOException;

/**
 * A container which creates {@linkplain FsController} file system controllers
 * and manages their life cycle.
 * <p>
 * Implementations should be thread-safe.
 *
 * @see    FsController
 * @see    FsModel
 * @author Christian Schlichtherle
 */
public interface FsManager {

    /**
     * Returns a new thread-safe archive file system controller.
     * This is a pure function without side effects.
     *
     * @param  driver the archive driver.
     * @param  model the file system model.
     * @param  parent the parent file system controller.
     * @return A new archive file system controller.
     */
    FsController newController(
            FsArchiveDriver<? extends FsArchiveEntry> driver,
            FsModel model,
            FsController parent);

    /**
     * Returns the thread-safe file system controller for the given mount point.
     * The life cycle of the returned file system controller gets managed by
     * this manager, i.e. it gets remembered for future lookup and
     * {@link #sync synchronization}.
     *
     * @param  driver the composite file system driver which shall get used to
     *         create a new file system controller if required.
     * @param  mountPoint the mount point of the file system.
     * @return The thread-safe file system controller for the given mount point.
     */
    FsController controller(FsMetaDriver driver, FsMountPoint mountPoint);

    /**
     * Uses the given visitor to {@link FsController#sync sync()} all managed
     * file system controllers.
     * If {@code sync()}ing a file system controller fails with an
     * {@link FsSyncException}, then the exception gets remembered and the loop
     * continues with {@code sync()}ing the remaining file system controllers.
     * Once the loop has completed, the exception(s) get processed for
     * (re)throwing based on their type and order of appearance.
     * <p>
     * Call this method instead of {@link #visit} for {@code sync()}ing in
     * order to support processing of additional aspects such as controlling a
     * shutdown hook, logging statistics et al.
     *
     * @param  visitor the visitor for
     *         {@linkplain FsSyncControllerVisitor#filter filtering} and
     *         {@linkplain FsSyncControllerVisitor#visit syncing}
     *         the managed file system controllers.
     * @throws FsSyncWarningException if <em>only</em> warning conditions
     *         apply.
     *         This implies that the respective file system controller has been
     *         {@link FsController#sync sync()}ed with constraints, e.g. if an
     *         open archive entry stream or channel gets forcibly
     *         {@link Closeable#close close()}d.
     * @throws FsSyncException if any error conditions apply.
     */
    void sync(FsSyncControllerVisitor visitor)
    throws FsSyncWarningException, FsSyncException;

    <X extends IOException> void visit(FsControllerVisitor<X> visitor) throws X;
}
