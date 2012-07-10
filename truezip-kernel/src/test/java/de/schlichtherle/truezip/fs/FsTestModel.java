/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.fs;

import javax.annotation.CheckForNull;
import javax.annotation.concurrent.ThreadSafe;

/**
 * @author Christian Schlichtherle
 */
@ThreadSafe
public class FsTestModel extends FsModel {

    private volatile boolean touched;

    public FsTestModel(  final FsMountPoint mountPoint,
                            final @CheckForNull FsModel parent) {
        super(mountPoint, parent);
    }

    @Override
    public boolean isTouched() {
        return touched;
    }

    @Override
    public void setTouched(final boolean touched) {
        this.touched = touched;
    }
}
