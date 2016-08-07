/*
 * Copyright (C) 2005-2015 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.kernel.spec;

import java.util.Objects;
import javax.annotation.CheckForNull;
import javax.annotation.concurrent.ThreadSafe;
import net.java.truecommons.shed.UniqueObject;

/**
 * An abstract file system model which does <em>not</em> implement the property
 * {@code touched}.
 * <p>
 * Subclasses should be thread-safe.
 *
 * @author Christian Schlichtherle
 */
@ThreadSafe
public abstract class FsAbstractModel
extends UniqueObject implements FsModel {

    private final FsMountPoint mountPoint;
    private @CheckForNull final FsModel parent;

    protected FsAbstractModel(
            final FsMountPoint mountPoint,
            final @CheckForNull FsModel parent) {
        if (!Objects.equals(mountPoint.getParent(),
                    (null == parent ? null : parent.getMountPoint())))
            throw new IllegalArgumentException("Parent/Member mismatch!");
        this.mountPoint = mountPoint;
        this.parent = parent;
    }

    @Override
    public final FsMountPoint getMountPoint() { return mountPoint; }

    @Override
    public final @CheckForNull FsModel getParent() { return parent; }

    /**
     * Returns a string representation of this object for debugging and logging
     * purposes.
     */
    @Override
    public String toString() {
        return String.format("%s@%x[mountPoint=%s, parent=%s, mounted=%b]",
                getClass().getName(),
                hashCode(),
                mountPoint,
                parent,
                isMounted());
    }
}
