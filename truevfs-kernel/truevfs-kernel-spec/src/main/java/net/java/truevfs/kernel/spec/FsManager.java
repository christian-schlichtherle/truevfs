/*
 * Copyright © 2005 - 2021 Schlichtherle IT Services.
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
     * Filters all managed file system controllers using the given
     * {@code filter} and accepts the given {@code visitor} to them.
     *
     * @param filter the filter for the managed file system controllers.
     *        Calls to its {@link Filter#accept(Object)} method should terminate
     *        quickly without an exception and must not have any side effects on
     *        the given controllers!
     * @param visitor the visitor of the filtered file system controllers.
     *        Calls to its {@link Visitor#visit(Object)} method may have side
     *        effects on the given controllers, e.g. by calling
     *        {@link FsController#sync(BitField)}.
     * @return {@code visitor}
     * @throws X at the discretion of the given visitor.
     *         Throwing this exception aborts the visiting.
     */
    <X extends Exception, V extends Visitor<? super FsController, X>>
    V accept(Filter<? super FsController> filter, V visitor) throws X;
}
