/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.kernel.spec;

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
extends FsAbstractManager {

    /** The nullable decorated file system manager. */
    protected @Nullable M manager;

    /**
     * Constructs a new decorating file system manager.
     *
     * @param manager the nullable file system manager to decorate.
     */
    protected FsDecoratingManager(final @CheckForNull M manager) {
        this.manager = manager;
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
            FsCompositeDriver driver,
            FsMountPoint mountPoint) {
        return manager.controller(driver, mountPoint);
    }

    @Override
    public int size() {
        return manager.size();
    }

    @Override
    public Iterator<FsController> iterator() {
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
