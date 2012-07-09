/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.fs;

import javax.annotation.concurrent.Immutable;

/**
 * An abstract file system controller which implements the {@link #getModel()}
 * method.
 *
 * @param  <M> the type of the file system model.
 * @since  TrueZIP 7.6
 * @author Christian Schlichtherle
 */
@Immutable
public abstract class FsAbstractController<M extends FsModel>
extends FsController<M>  {

    private final M model;

    /**
     * Constructs a new file system controller for the given model.
     * 
     * @param model the file system model.
     */
    protected FsAbstractController(final M model) {
        if (null == (this.model = model))
            throw new NullPointerException();
    }

    @Override
    public final M getModel() {
        return model;
    }

    public final FsMountPoint getMountPoint() {
        return model.getMountPoint();
    }

    public final boolean isTouched() {
        return model.isTouched();
    }

    protected final void setTouched(boolean touched) {
        model.setTouched(touched);
    }
}
