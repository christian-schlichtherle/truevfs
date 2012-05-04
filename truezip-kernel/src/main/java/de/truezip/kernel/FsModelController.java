/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.truezip.kernel;

import java.util.Objects;

/**
 * An abstract file system controller which implements the {@link #getModel()}
 * method.
 *
 * @param  <M> the type of the file system model.
 * @author Christian Schlichtherle
 */
public abstract class FsModelController<M extends FsModel>
extends FsAbstractController<M>  {

    private final M model;

    /**
     * Constructs a new file system controller for the given model.
     * 
     * @param model the file system model.
     */
    protected FsModelController(final M model) {
        this.model = Objects.requireNonNull(model);
    }

    @Override
    public final M getModel() {
        return model;
    }

    /**
     * Returns the mount point of this (federated virtual) file system as
     * defined by the {@linkplain #getModel() model}.
     * 
     * @return The mount point of this (federated virtual) file system as
     *         defined by the {@linkplain #getModel() model}.
     */
    protected final FsMountPoint getMountPoint() {
        return model.getMountPoint();
    }

    /**
     * Returns the {@code touched} property of the
     * {@linkplain #getModel() file system model}.
     * 
     * @return the {@code touched} property of the
     *         {@linkplain #getModel() file system model}.
     */
    protected final boolean isTouched() {
        return model.isTouched();
    }

    /**
     * Sets the {@code touched} property of the
     * {@linkplain #getModel() file system model}.
     * 
     * @param touched the {@code touched} property of the
     *         {@linkplain #getModel() file system model}.
     */
    protected final void setTouched(boolean touched) {
        model.setTouched(touched);
    }
}
