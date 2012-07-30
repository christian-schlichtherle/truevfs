/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.kernel.spec;

import java.util.Iterator;
import net.java.truecommons.shed.BitField;

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
     * Returns a new thread-safe archive file system controller.
     * This is a pure function without side effects.
     *
     * @param  <E> the type of the archive entries.
     * @param  driver the archive driver.
     * @param  model the file system model.
     * @param  parent the parent file system controller.
     * @return A new archive file system controller.
     */
    <E extends FsArchiveEntry> FsController<? extends FsModel> newController(
            FsArchiveDriver<E> driver,
            FsModel model,
            FsController<? extends FsModel> parent);

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
    FsController<? extends FsModel> controller(
            FsCompositeDriver driver,
            FsMountPoint mountPoint);

    /**
     * Returns the number of managed file system controllers.
     *
     * @return The number of managed file system controllers.
     */
    int size();

    /**
     * Returns an ordered iterator for the managed file system controllers.
     * The iterated file system controllers are ordered so that all file
     * systems appear before any of their parent file systems.
     * Last, but not least: The iterator must be consistent in multithreaded
     * environments!
     *
     * @return An ordered iterator for the managed file system controllers.
     */
    @Override
    Iterator<FsController<? extends FsModel>> iterator();

    /**
     * Calls {@link FsController#sync(BitField)} on all managed file system
     * controllers.
     * If sync()ing a file system controller fails with an
     * {@link FsSyncException}, then the exception gets remembered and the loop
     * continues with sync()ing the remaining file system controllers.
     * After the loop, the exception(s) get processed for (re)throwing based
     * on their type and order of appearance.
     *
     * @param  options the options for synchronizing the file system.
     * @throws FsSyncWarningException if <em>only</em> warning conditions
     *         apply.
     *         This implies that the respective file system controller has been
     *         synchronized with constraints, e.g. if an unclosed archive entry
     *         stream gets forcibly closed.
     * @throws FsSyncException if any error conditions apply.
     * @throws IllegalArgumentException if the combination of synchronization
     *         options is illegal, e.g. if {@link FsSyncOption#ABORT_CHANGES}
     *         is set.
     */
    void sync(final BitField<FsSyncOption> options)
    throws FsSyncWarningException, FsSyncException;
}
