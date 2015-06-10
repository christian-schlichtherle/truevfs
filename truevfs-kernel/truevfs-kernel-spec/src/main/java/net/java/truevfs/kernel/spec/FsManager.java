/*
 * Copyright (C) 2005-2015 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.kernel.spec;

import net.java.truecommons.shed.BitField;
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
extends FsModel.Factory<FsDriver>,
        FsController.Factory<FsArchiveDriver<? extends FsArchiveEntry>>{

    /**
     * Returns the thread-safe file system controller for the given mount point.
     * The life cycle of the returned file system controller gets managed by
     * this manager, i.e. it gets remembered for future lookup and
     * {@link FsController#sync synchronization}.
     *
     * @param  driver the composite file system driver which shall get used to
     *         create a new file system controller if required.
     * @param  mountPoint the mount point of the file system.
     * @return The thread-safe file system controller for the given mount point.
     */
    FsController controller(FsCompositeDriver driver, FsMountPoint mountPoint);

    /**
     * Invokes the given {@code visitor} on all managed file system controllers
     * which get accepted by the given {@code filter}.
     *
     * @param filter the filter for the managed file system controllers.
     *        Calling this object must not have any observable side effects!
     * @param visitor the visitor for the filtered file system controllers.
     *        Calling this object may have an observable side effect, e.g.
     *        {@link FsController#sync(BitField)}.
     * @throws Exception at the discretion of the visitor.
     *         This will abort the visiting.
     */
    <X extends Exception> void accept(
            Filter<? super FsController> filter,
            Visitor<? super FsController, X> visitor)
            throws X;
}
