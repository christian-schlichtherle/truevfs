/*
 * Copyright 2004-2012 Schlichtherle IT Services
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package de.schlichtherle.truezip.fs;

import javax.annotation.concurrent.ThreadSafe;

/**
 * An abstract decorator for a file system model.
 *
 * @param   <M> The type of the decorated file system model.
 * @author  Christian Schlichtherle
 * @version $Id$
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
        if (null == delegate)
            throw new NullPointerException();
        this.delegate = delegate;
    }

    @Override
    public FsMountPoint getMountPoint() {
        return delegate.getMountPoint();
    }

    @Override
    public FsModel getParent() {
        return delegate.getParent();
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
        return new StringBuilder()
                .append(getClass().getName())
                .append("[delegate=")
                .append(delegate)
                .append("]")
                .toString();
    }
}
