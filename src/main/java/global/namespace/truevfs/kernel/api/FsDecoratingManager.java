/*
 * Copyright © 2005 - 2021 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package global.namespace.truevfs.kernel.api;

import global.namespace.truevfs.comp.util.Filter;
import global.namespace.truevfs.comp.util.Visitor;

import java.util.Objects;
import java.util.Optional;

/**
 * An abstract decorator for a file system manager.
 *
 * @author Christian Schlichtherle
 */
public abstract class FsDecoratingManager extends FsAbstractManager {

    /** The decorated file system manager. */
    protected final FsManager manager;

    protected FsDecoratingManager(final FsManager manager) {
        this.manager = Objects.requireNonNull(manager);
    }

    @Override
    public FsModel newModel(
            FsDriver context,
            FsMountPoint mountPoint,
            Optional<? extends FsModel> parent) {
        assert mountPoint.getParent().equals(parent.map(FsModel::getMountPoint));
        return manager.newModel(context, mountPoint, parent);
    }

    @Override
    public FsController newController(
            FsArchiveDriver<? extends FsArchiveEntry> context,
            FsModel model,
            Optional<? extends FsController> parent) {
        assert model.getParent().equals(parent.map(FsController::getModel));
        return manager.newController(context, model, parent);
    }

    @Override
    public FsController controller(FsCompositeDriver driver, FsMountPoint mountPoint) {
        return manager.controller(driver, mountPoint);
    }

    @Override
    public <X extends Exception, V extends Visitor<? super FsController, X>> V accept(final Filter<? super FsController> filter, V visitor) throws X {
        return manager.accept(filter, visitor);
    }

    /**
     * Returns a string representation of this object for debugging and logging
     * purposes.
     */
    @Override
    public String toString() {
        return String.format("%s@%x[manager=%s]",
                getClass().getName(),
                hashCode(),
                manager);
    }
}
