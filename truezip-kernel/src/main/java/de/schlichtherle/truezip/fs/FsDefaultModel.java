/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.fs;

import de.schlichtherle.truezip.fs.addr.FsMountPoint;
import javax.annotation.CheckForNull;
import javax.annotation.concurrent.ThreadSafe;

/**
 * The default implementation of a file system model for non-federated file
 * systems.
 *
 * @author Christian Schlichtherle
 */
@ThreadSafe
class FsDefaultModel extends FsModel {

    private final FsMountPoint mountPoint;
    private final @CheckForNull FsModel parent;

    FsDefaultModel(  final FsMountPoint mountPoint,
                            final @CheckForNull FsModel parent) {
        if (!equals(mountPoint.getParent(),
                    (null == parent ? null : parent.getMountPoint())))
            throw new IllegalArgumentException("Parent/Member mismatch!");
        this.mountPoint = mountPoint;
        this.parent = parent;
    }

    private static boolean equals(@CheckForNull Object o1, @CheckForNull Object o2) {
        return o1 == o2 || null != o1 && o1.equals(o2);
    }

    @Override
    public final FsMountPoint getMountPoint() {
        return mountPoint;
    }

    @Override
    public final FsModel getParent() {
        return parent;
    }
}
