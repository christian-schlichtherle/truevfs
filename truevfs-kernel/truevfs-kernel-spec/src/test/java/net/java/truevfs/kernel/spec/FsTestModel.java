/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.kernel.spec;

import net.java.truevfs.kernel.spec.FsModel;
import net.java.truevfs.kernel.spec.FsAbstractModel;
import net.java.truevfs.kernel.spec.FsMountPoint;
import javax.annotation.CheckForNull;
import javax.annotation.concurrent.ThreadSafe;

/**
 * @author Christian Schlichtherle
 */
@ThreadSafe
public class FsTestModel extends FsAbstractModel {

    private volatile boolean mounted;

    FsTestModel(FsMountPoint mountPoint, @CheckForNull FsModel parent) {
        super(mountPoint, parent);
    }

    @Override
    public boolean isMounted() {
        return mounted;
    }

    @Override
    public void setMounted(final boolean mounted) {
        this.mounted = mounted;
    }
}
