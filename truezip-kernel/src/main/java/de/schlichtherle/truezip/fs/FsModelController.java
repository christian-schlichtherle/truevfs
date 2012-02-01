/*
 * Copyright 2004-2012 Schlichtherle IT Services
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package de.schlichtherle.truezip.fs;

import net.jcip.annotations.Immutable;

/**
 * An abstract file system controller which implements the {@link #getModel()}
 * method so that it can forward calls to its additional protected methods to
 * this model for the convenience of sub-classes.
 *
 * @since   TrueZIP 7.2
 * @author  Christian Schlichtherle
 * @version $Id$
 */
@Immutable
public abstract class FsModelController<M extends FsModel>
extends FsController<M>  {

    private final M model;

    /**
     * Constructs a new file system controller for the given model.
     * 
     * @param model the file system model.
     */
    protected FsModelController(final M model) {
        if (null == model)
            throw new NullPointerException();
        this.model = model;
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
