/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.truevfs.kernel;

import java.util.Iterator;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;

/**
 * An abstract decorator for a file system manager.
 * 
 * @param  <M> the type of the decorated file system manager.
 * @author Christian Schlichtherle
 */
@ThreadSafe
public abstract class FsDecoratingManager<M extends FsManager>
extends FsManager {

    /** The nullable decorated file system manager. */
    protected @Nullable M manager;

    /**
     * Constructs a new decorating file system manager.
     *
     * @param manager the file system manager to decorate.
     */
    protected FsDecoratingManager(final @CheckForNull M manager) {
        this.manager = manager;
    }

    @Override
    public FsController<? extends FsModel>
    controller(FsCompositeDriver driver, FsMountPoint mountPoint) {
        return manager.controller(driver, mountPoint);
    }

    @Override
    public FsController<? extends FsModel> newController(
            FsArchiveDriver<? extends FsArchiveEntry> driver,
            FsModel model,
            FsController<? extends FsModel> parent) {
        assert false : "This method should never get called on this class!";
        return manager.newController(driver, model, parent);
    }

    @Override
    public int size() {
        return manager.size();
    }

    @Override
    public Iterator<FsController<? extends FsModel>> iterator() {
        return manager.iterator();
    }

    /**
     * Returns a string representation of this object for debugging and logging
     * purposes.
     */
    @Override
    public String toString() {
        return String.format("%s[manager=%s]",
                getClass().getName(),
                manager);
    }
}
