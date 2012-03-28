/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.truezip.kernel.fs;

import de.truezip.kernel.fs.addr.FsMountPoint;
import java.util.Iterator;
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

    /** The decorated file system manager. */
    protected final M delegate;

    /**
     * Constructs a new decorating file system manager.
     *
     * @param delegate the file system manager to decorate.
     */
    protected FsDecoratingManager(final M delegate) {
        if (null == delegate)
            throw new NullPointerException();
        this.delegate = delegate;
    }

    @Override
    public FsController<?>
    getController(FsMountPoint mountPoint, FsCompositeDriver driver) {
        return delegate.getController(mountPoint, driver);
    }

    @Override
    public <E extends FsArchiveEntry> FsController<?>
    newController(  FsArchiveDriver<E> driver,
                    FsModel model,
                    FsController<?> parent) {
        assert false : "This method should never get called on this class!";
        return delegate.newController(driver, model, parent);
    }

    @Override
    public int size() {
        return delegate.size();
    }

    @Override
    public Iterator<FsController<?>> iterator() {
        return delegate.iterator();
    }

    /**
     * Returns a string representation of this object for debugging and logging
     * purposes.
     */
    @Override
    public String toString() {
        return String.format("%s[delegate=%s]",
                getClass().getName(),
                delegate);
    }
}
