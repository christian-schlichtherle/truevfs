/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.truevfs.kernel.spec;

import java.util.Iterator;
import javax.annotation.concurrent.ThreadSafe;
import static net.truevfs.kernel.spec.FsSyncOption.ABORT_CHANGES;
import net.truevfs.kernel.spec.util.BitField;
import net.truevfs.kernel.spec.util.UniqueObject;

/**
 * An abstract container which manages the life cycle of controllers for
 * federated file systems.
 * A file system is federated if and only if it's a member of a parent
 * (virtual) file system.
 * <p>
 * Sub-classes must be thread-safe, too.
 *
 * @author Christian Schlichtherle
 */
@ThreadSafe
public abstract class FsManager
extends UniqueObject implements Iterable<FsController<? extends FsModel>> {

    /**
     * Returns a thread-safe file system controller for the given mount point.
     * If and only if the given mount point addresses a federated file system,
     * the returned file system controller is remembered for life cycle
     * management, i.e. future lookup and {@link #sync synchronization}
     * operations.
     *
     * @param  mountPoint the mount point of the file system.
     * @param  driver the file system composite driver which shall get used to
     *         create a new file system controller if required.
     * @return A thread-safe file system controller for the given mount point.
     */
    public abstract FsController<? extends FsModel> controller(
            FsCompositeDriver driver,
            FsMountPoint mountPoint);

    /**
     * Returns a new archive file system controller.
     * 
     * @param driver the archive driver.
     * @param model the file system model.
     * @param parent the parent file system controller.
     * @return A new archive file system controller.
     */
    public abstract FsController<? extends FsModel> newController(
            FsArchiveDriver<? extends FsArchiveEntry> driver,
            FsModel model,
            FsController<? extends FsModel> parent);

    /**
     * Returns the number of federated file systems managed by this instance.
     *
     * @return The number of federated file systems managed by this instance.
     */
    public abstract int size();

    /**
     * Returns an iterator over the controllers of all federated file systems
     * managed by this instance in sorted order.
     * The iterated file system controllers must be ordered so that all file
     * systems appear before any of their parent file systems.
     * Last, but not least: The iterator must be consistent in multithreaded
     * environments!
     *
     * @return An iterator over the controllers of all federated file systems
     *         managed by this instance in sorted order.
     */
    @Override
    public abstract Iterator<FsController<? extends FsModel>> iterator();

    /**
     * Commits all unsynchronized changes to the contents of all federated file
     * systems managed by this instance to their respective parent file system,
     * releases the associated resources (e.g. target archive files) for
     * access by third parties (e.g. other processes), cleans up any temporary
     * allocated resources (e.g. temporary files) and purges any cached data.
     * Note that temporary resources may get allocated even if the federated
     * file systems were accessed read-only.
     * As a side effect, this will reset the state of the respective file
     * system controllers.
     *
     * @param  options the options for synchronizing the file system.
     * @throws FsSyncWarningException if <em>only</em> warning conditions
     *         apply.
     *         This implies that the respective parent file system has been
     *         synchronized with constraints, e.g. if an unclosed archive entry
     *         stream gets forcibly closed.
     * @throws FsSyncException if any error conditions apply.
     * @throws IllegalArgumentException if the combination of synchronization
     *         options is illegal, e.g. if {@link FsSyncOption#ABORT_CHANGES}
     *         is set.
     */
    public void sync(final BitField<FsSyncOption> options)
    throws FsSyncWarningException, FsSyncException {
        if (options.get(ABORT_CHANGES))
            throw new IllegalArgumentException();
        final FsSyncExceptionBuilder builder = new FsSyncExceptionBuilder();
        for (final FsController<?> controller : this) {
            try {
                controller.sync(options);
            } catch (final FsSyncException ex) {
                builder.warn(ex);
            }
        }
        builder.check();
    }

    /**
     * Returns a string representation of this object for debugging and logging
     * purposes.
     */
    @Override
    public String toString() {
        return String.format("%s[size=%d]",
                getClass().getName(),
                size());
    }
}
