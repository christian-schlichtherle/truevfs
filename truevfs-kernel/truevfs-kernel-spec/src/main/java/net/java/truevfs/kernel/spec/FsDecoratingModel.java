/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.kernel.spec;

import javax.annotation.concurrent.ThreadSafe;

/**
 * An abstract decorator for a file system model.
 *
 * @author Christian Schlichtherle
 */
@ThreadSafe
public abstract class FsDecoratingModel extends FsAbstractModel {

    /** The decorated file system model. */
    protected final FsModel model;

    protected FsDecoratingModel(final FsModel model) {
        super(model.getMountPoint(), model.getParent());
        this.model = model;
    }

    @Override
    public boolean isMounted() {
        return model.isMounted();
    }

    @Override
    public void setMounted(boolean touched) {
        model.setMounted(touched);
    }

    /**
     * Returns a string representation of this object for debugging and logging
     * purposes.
     */
    @Override
    public String toString() {
        return String.format("%s@%x[model=%s]",
                getClass().getName(),
                hashCode(),
                model);
    }
}
