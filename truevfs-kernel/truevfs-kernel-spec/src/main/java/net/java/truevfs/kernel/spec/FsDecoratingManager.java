/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.kernel.spec;

import java.util.Objects;
import javax.annotation.concurrent.ThreadSafe;
import net.java.truecommons.shed.BitField;
import net.java.truecommons.shed.Filter;

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
    public final FsController newController(
            FsArchiveDriver<? extends FsArchiveEntry> driver,
            FsModel model,
            FsController parent) {
        assert false : "This method should never get called on this class!";
        return manager.newController(driver, model, parent);
    }

    @Override
    public FsController controller(
            FsMetaDriver driver,
            FsMountPoint mountPoint) {
        return manager.controller(driver, mountPoint);
    }

    @Override
    public FsControllerStream controllers(Filter<? super FsController> filter) {
        return manager.controllers(filter);
    }

    @Override
    public void sync(
            BitField<FsSyncOption> options,
            Filter<? super FsController> filter)
    throws FsSyncWarningException, FsSyncException {
        manager.sync(options, filter);
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
