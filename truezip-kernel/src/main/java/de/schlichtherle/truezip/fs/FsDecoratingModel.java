/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.fs;

import javax.annotation.concurrent.ThreadSafe;

/**
 * An abstract decorator for a file system model.
 *
 * @param  <M> the type of the decorated file system model.
 * @author Christian Schlichtherle
 */
@ThreadSafe
public abstract class FsDecoratingModel<M extends FsModel> extends FsModel {

    /** The decorated file system model. */
    protected final M delegate;

    /**
     * Constructs a new decorating file system model.
     *
     * @param delegate the file system model to decorate.
     */
    protected FsDecoratingModel(final M delegate) {
        super(delegate.getMountPoint(), delegate.getParent());
        this.delegate = delegate;
    }

    @Override
    public boolean isTouched() {
        return delegate.isTouched();
    }

    @Override
    public void setTouched(boolean touched) {
        delegate.setTouched(touched);
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
