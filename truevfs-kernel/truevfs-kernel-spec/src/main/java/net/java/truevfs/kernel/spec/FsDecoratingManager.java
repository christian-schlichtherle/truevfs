/*
 * Copyright (C) 2005-2015 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.kernel.spec;

import java.io.IOException;
import java.util.Objects;
import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;
import net.java.truecommons.shed.*;

/**
 * An abstract decorator for a file system manager.
 *
 * @author Christian Schlichtherle
 */
@ThreadSafe
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
            FsModel parent) {
        assert null == parent
                    ? null == mountPoint.getParent()
                    : parent.getMountPoint().equals(mountPoint.getParent());
        return manager.newModel(context, mountPoint, parent);
    }

    @Override
    public FsController newController(
            FsArchiveDriver<? extends FsArchiveEntry> context,
            FsModel model,
            @Nullable FsController parent) {
        assert null == parent
                    ? null == model.getParent()
                    : parent.getModel().equals(model.getParent());
        return manager.newController(context, model, parent);
    }

    @Override
    public FsController controller(
            FsCompositeDriver driver,
            FsMountPoint mountPoint) {
        return manager.controller(driver, mountPoint);
    }

    @Override
    public void sync(
            Filter<? super FsController> filter,
            Visitor<? super FsController, FsSyncException> visitor)
    throws FsSyncException {
        manager.sync(filter, visitor);
    }

    @Override
    public <X extends Exception> void accept(
            Filter<? super FsController> filter,
            Visitor<? super FsController, X> visitor)
    throws X {
        manager.accept(filter, visitor);
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
