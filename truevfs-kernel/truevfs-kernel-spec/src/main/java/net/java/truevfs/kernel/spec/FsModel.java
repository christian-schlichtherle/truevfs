/*
 * Copyright (C) 2005-2013 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.kernel.spec;

import javax.annotation.CheckForNull;
import net.java.truecommons.shed.ImplementationsShouldExtend;

/**
 * Defines common properties of any file system.
 * <p>
 * Implementations should be thread-safe.
 *
 * @see    FsController
 * @see    FsManager
 * @author Christian Schlichtherle
 */
@ImplementationsShouldExtend(FsAbstractModel.class)
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
     * <p>
     * An implementation may always return {@code false} if the associated
     * file system controller is stateless.
     *
     * @return {@code true} if and only if some state associated with the
     *         federated file system has been modified so that the
     *         corresponding {@link FsController} must not get discarded until
     *         the next {@link FsController#sync sync}.
     */
    boolean isMounted();

    /**
     * Sets the value of the property {@link #isMounted() mounted}.
     * Only file system controllers should call this method in order to
     * register themselves for a call their {@link FsController#sync} method.
     * <p>
     * An implementation may ignore calls to this method if the associated
     * file system controller is stateless.
     *
     * @param mounted the new value of this property.
     */
    void setMounted(boolean mounted);
}
