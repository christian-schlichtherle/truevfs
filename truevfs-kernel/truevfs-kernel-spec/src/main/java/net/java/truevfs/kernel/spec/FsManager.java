/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.kernel.spec;

import java.io.Closeable;
import java.io.IOException;
import net.java.truecommons.shed.Filter;

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
     * file system controllers which get accepted by the visitor's
     * {@link FsControllerSyncVisitor#filter}.
     * <p>
     * If possible, clients should set the visitor's
     * {@linkplain FsControllerSyncVisitor#filter filter}
     * to {@link Filter#ACCEPT_ANY} in order to enable the implementation to
     * perform additional cleanup operations, e.g. removing a shutdown hook.
     * <p>
     * The visitor's
     * {@linkplain FsControllerSyncVisitor#builder sync exception builder}
     * gets used to process any {@link FsSyncException}s while iterating over
     * all managed file system controllers.
     * <p>
     * Call this method instead of {@link #visit} for {@code sync()}ing in
     * order to support processing of additional aspects such as controlling a
     * shutdown hook, logging statistics et al.
     *
     * @param  visitor the visitor for
     *         {@linkplain FsControllerSyncVisitor#filter filtering} and
     *         {@linkplain FsControllerSyncVisitor#visit syncing}
     *         the managed file system controllers.
     * @throws FsSyncWarningException if <em>only</em> warning conditions
     *         apply.
     *         This implies that the respective file system controller has been
     *         {@link FsController#sync sync()}ed with constraints, e.g. if an
     *         open archive entry stream or channel gets forcibly
     *         {@link Closeable#close close()}d.
     * @throws FsSyncException if any error conditions apply.
     */
    void sync(FsControllerSyncVisitor visitor)
    throws FsSyncWarningException, FsSyncException;

    /**
     * Uses the given visitor to call an operation on all managed
     * file system controllers which get accepted by the visitor's
     * {@link FsControllerVisitor#filter}.
     * This is the engine for calls to {@link #sync}.
     * <p>
     * The visitor's
     * {@linkplain FsControllerVisitor#builder exception builder}
     * gets used to process any {@link IOException}s while iterating over
     * all managed file system controllers.
     *
     * @param  visitor the visitor for
     *         {@linkplain FsControllerVisitor#filter filtering} and
     *         {@linkplain FsControllerVisitor#visit syncing}
     *         the managed file system controllers.
     * @throws FsSyncWarningException if <em>only</em> warning conditions
     *         apply.
     *         This implies that the respective file system controller has been
     *         {@link FsController#sync sync()}ed with constraints, e.g. if an
     *         open archive entry stream or channel gets forcibly
     *         {@link Closeable#close close()}d.
     * @throws FsSyncException if any error conditions apply.
     */
    <X extends IOException> void visit(FsControllerVisitor<X> visitor) throws X;
}
