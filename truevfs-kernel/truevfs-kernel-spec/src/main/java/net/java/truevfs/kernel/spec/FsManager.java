/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.kernel.spec;

import java.io.Closeable;
import java.io.IOException;
import net.java.truecommons.shed.Filter;
import net.java.truecommons.shed.ImplementationsShouldExtend;
import net.java.truecommons.shed.Visitor;

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
@ImplementationsShouldExtend(FsAbstractManager.class)
public interface FsManager
extends FsModelFactory<FsDriver>,
        FsControllerFactory<FsArchiveDriver<? extends FsArchiveEntry>>{

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
    FsController controller(FsCompositeDriver driver, FsMountPoint mountPoint);

    /**
     * Uses the given visitor to {@link FsController#sync sync()} all managed
     * file system controllers which get accepted by the given {@code filter}.
     * <p>
     * Call this method instead of {@link #visit} for {@code sync()}ing in
     * order to support processing of additional aspects such as controlling a
     * shutdown hook, logging statistics et al.
     * <p>
     * The implementation needs to use an {@link FsSyncExceptionBuilder} while
     * iterating over all managed file system controllers in order to ensure
     * that all controllers get synced, even if one or more controllers fail
     * with an {@link FsSyncException}.
     *
     * @param  filter the filter for the managed file system controllers.
     *         If set to {@link Filter#ACCEPT_ANY}, then the implementation may
     *         perform additional cleanup operations, e.g. remove a shutdown
     *         hook.
     * @param  visitor the visitor for syncing the filtered file system
     *         controllers.
     * @throws FsSyncWarningException if <em>only</em> warning conditions
     *         apply.
     *         This implies that the respective file system controller has been
     *         {@link FsController#sync sync()}ed with constraints, e.g. if an
     *         open archive entry stream or channel gets forcibly
     *         {@link Closeable#close close()}d.
     * @throws FsSyncException if any error conditions apply.
     */
    void sync(
            Filter<? super FsController> filter,
            Visitor<? super FsController, FsSyncException> visitor)
    throws FsSyncException;

    /**
     * Uses the given visitor to call an operation on all managed
     * file system controllers which get accepted by the given {@code filter}.
     * This is the engine for calls to {@link #sync}.
     *
     * @param  filter the filter for the managed file system controllers.
     * @param  visitor the visitor for the filtered file system controllers.
     * @throws IOException at the discretion of the visitor.
     *         This will abort the visiting.
     */
    <X extends IOException> void visit(
            Filter<? super FsController> filter,
            Visitor<? super FsController, X> visitor)
    throws X;
}
