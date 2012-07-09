/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.truevfs.kernel.spec;

import java.util.Iterator;
import net.truevfs.kernel.spec.util.BitField;

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
public interface FsManager extends Iterable<FsController<? extends FsModel>> {

    /**
     * Returns the number of archive file systems managed by this instance.
     *
     * @return The number of archive file systems managed by this instance.
     */
    int size();

    /**
     * Returns an iterator over the controllers of all archive file systems
     * managed by this instance in reverse order.
     * The iterated file system controllers must be ordered so that all file
     * systems appear before any of their parent file systems.
     * Last, but not least: The iterator must be consistent in multithreaded
     * environments!
     *
     * @return An iterator over the controllers of all archive file systems
     *         managed by this instance in reverse order.
     */
    @Override
    Iterator<FsController<? extends FsModel>> iterator();

    /**
     * Returns a new archive file system controller.
     * This is pure function without side effects.
     *
     * @param  driver the archive driver.
     * @param  model the file system model.
     * @param  parent the parent file system controller.
     * @return A new archive file system controller.
     */
    FsController<? extends FsModel> newController(
            FsArchiveDriver<? extends FsArchiveEntry> driver,
            FsModel model,
            FsController<? extends FsModel> parent);

    /**
     * Returns a thread-safe file system controller for the given mount point.
     * If and only if the given mount point addresses an archive file system,
     * the life cycle of the returned file system controller gets managed by
     * this instance, i.e. it gets remembered for future lookup and
     * {@link #sync synchronization}.
     *
     * @param  mountPoint the mount point of the file system.
     * @param  driver the composite file system driver which shall get used to
     *         create a new file system controller if required.
     * @return A thread-safe file system controller for the given mount point.
     */
    FsController<? extends FsModel> controller(
            FsCompositeDriver driver,
            FsMountPoint mountPoint);

    /**
     * Commits all unsynchronized changes to the contents of all archive file
     * systems managed by this instance to their respective parent file system,
     * releases the associated resources (e.g. target archive files) for
     * access by third parties (e.g. other processes), cleans up any temporary
     * allocated resources (e.g. temporary files) and purges any cached data.
     * Note that temporary resources may get allocated even if the archive file
     * systems were accessed read-only.
     * As a side effect, this will reset the state of the respective file
     * system controllers.
     *
     * @param  options the options for synchronizing the file system.
     * @throws FsSyncWarningException if <em>only</em> warning conditions
     * apply.
     * This implies that the respective parent file system has been
     * synchronized with constraints, e.g. if an unclosed archive entry
     * stream gets forcibly closed.
     * @throws FsSyncException if any error conditions apply.
     * @throws IllegalArgumentException if the combination of synchronization
     * options is illegal, e.g. if {@link FsSyncOption#ABORT_CHANGES}
     * is set.
     */
    void sync(final BitField<FsSyncOption> options)
    throws FsSyncWarningException, FsSyncException;
}
