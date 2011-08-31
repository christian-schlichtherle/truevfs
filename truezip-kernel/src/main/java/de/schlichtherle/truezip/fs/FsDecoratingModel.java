/*
 * Copyright (C) 2011 Schlichtherle IT Services
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package de.schlichtherle.truezip.fs;

import edu.umd.cs.findbugs.annotations.DefaultAnnotation;
import edu.umd.cs.findbugs.annotations.NonNull;
import net.jcip.annotations.ThreadSafe;

/**
 * An abstract decorator for a file system model.
 *
 * @param   <M> The type of the decorated file system model.
 * @author  Christian Schlichtherle
 * @version $Id$
 */
@ThreadSafe
@DefaultAnnotation(NonNull.class)
public abstract class FsDecoratingModel<M extends FsModel> extends FsModel {

    protected final M delegate;

    protected FsDecoratingModel(final M model) {
        if (null == model)
            throw new NullPointerException();
        this.delegate = model;
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
