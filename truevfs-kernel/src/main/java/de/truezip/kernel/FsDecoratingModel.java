/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.truezip.kernel;

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
    protected final M model;

    /**
     * Constructs a new decorating file system model.
     *
     * @param model the file system model to decorate.
     */
    protected FsDecoratingModel(final M model) {
        super(model.getMountPoint(), model.getParent());
        this.model = model;
    }

    @Override
    public boolean isTouched() {
        return model.isTouched();
    }

    @Override
    public void setTouched(boolean touched) {
        model.setTouched(touched);
    }

    /**
     * Returns a string representation of this object for debugging and logging
     * purposes.
     */
    @Override
    public String toString() {
        return String.format("%s[model=%s]",
                getClass().getName(),
                model);
    }
}
