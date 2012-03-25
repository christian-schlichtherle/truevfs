/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.truezip.kernel.fs;

import de.truezip.kernel.fs.addr.FsMountPoint;
import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;

/**
 * Defines the common properties of a file system.
 * <p>
 * Sub-classes must be thread-safe, too.
 *
 * @see    FsController
 * @author Christian Schlichtherle
 */
@ThreadSafe
public abstract class FsModel {

    /**
     * Returns the mount point of the file system.
     * <p>
     * The mount point may be used to construct error messages or to locate
     * and access file system meta data which is stored outside the file system,
     * e.g. in-memory stored passwords for RAES encrypted ZIP files.
     *
     * @return The mount point of the file system.
     */
    public abstract FsMountPoint getMountPoint();

    /**
     * Returns the model of the parent file system or {@code null} if and
     * only if the file system is not federated, i.e. if it's not a member of
     * a parent file system.
     *
     * @return The nullable parent file system model.
     */
    @Nullable
    public abstract FsModel getParent();

    /**
     * Returns {@code true} if and only if some state associated with the
     * federated file system has been modified so that the
     * corresponding {@link FsController} must not get discarded until
     * the next {@link FsController#sync sync}.
     * <p>
     * The implementation in the class {@link FsModel} always returns
     * {@code false}.
     * 
     * @return {@code true} if and only if some state associated with the
     *         federated file system has been modified so that the
     *         corresponding {@link FsController} must not get discarded until
     *         the next {@link FsController#sync sync}.
     */
    public boolean isTouched() {
        return false;
    }

    /**
     * Sets the value of the property {@link #isTouched() touched}
     * (optional operation).
     * <p>
     * The implementation in the class {@link FsModel} always throws an
     * {@link UnsupportedOperationException}.
     *
     * @param  touched the new value of this property.
     * @throws UnsupportedOperationException At the discretion of the
     *         implementation, e.g. if the file system type does not support
     *         {@link FsController#sync syncing}.
     */
    public void setTouched(boolean touched) {
        throw new UnsupportedOperationException();
    }

    /**
     * Two file system models are considered equal if and only if they are
     * identical.
     * This can't get overriden.
     * 
     * @param that the object to compare.
     */
    @Override
    @SuppressWarnings("EqualsWhichDoesntCheckParameterClass")
    public final boolean equals(Object that) {
        return this == that;
    }

    /**
     * Returns a hash code which is consistent with {@link #equals}.
     * This can't get overriden.
     */
    @Override
    public final int hashCode() {
        return super.hashCode();
    }

    /**
     * Returns a string representation of this object for debugging and logging
     * purposes.
     */
    @Override
    public String toString() {
        return String.format("%s[mountPoint=%s, parent=%s, touched=%b]",
                getClass().getName(),
                getMountPoint(),
                getParent(),
                isTouched());
    }
}