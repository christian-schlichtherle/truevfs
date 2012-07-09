/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.truevfs.kernel.spec;

import javax.annotation.CheckForNull;

/**
 * Defines common properties of any file system.
 * <p>
 * Implementations should be thread-safe.
 * 
 * @see    FsController
 * @see    FsManager
 * @author Christian Schlichtherle
 */
public interface FsModel {

    /**
     * Returns the mount point of the file system.
     * The mount point may be used to construct error messages or to locate
     * and access file system meta data which is stored outside the file system,
     * e.g. passwords for RAES encrypted ZIP files.
     *
     * @return The mount point of the file system.
     */
    FsMountPoint getMountPoint();

    /**
     * Returns the parent file system model or {@code null} if and only if the
     * file system is not federated, i.e. if it's not a member of a parent file
     * system.
     *
     * @return The nullable parent file system model.
     */
    @CheckForNull FsModel getParent();

    /**
     * Returns {@code true} if and only if some state associated with the
     * federated file system has been modified so that the
     * corresponding {@link FsController} must not get discarded until
     * the next call to {@link FsController#sync sync}.
     * 
     * @return {@code true} if and only if some state associated with the
     *         federated file system has been modified so that the
     *         corresponding {@link FsController} must not get discarded until
     *         the next {@link FsController#sync sync}.
     */
    boolean isTouched();

    /**
     * Sets the value of the property {@link #isTouched() touched}.
     *
     * @param  touched the new value of this property.
     * @throws UnsupportedOperationException If the file system is not
     *         federated, i.e. if it does not have a parent file system.
     */
    void setTouched(boolean touched);
}
