/*
 * Copyright © 2005 - 2021 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package global.namespace.truevfs.kernel.spec;

import global.namespace.truevfs.comp.shed.ImplementationsShouldExtend;

import java.util.Optional;

/**
 * Defines common properties of any file system.
 * <p>
 * Implementations should be safe for multi-threaded access.
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
    Optional<? extends FsModel> getParent();

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

    /**
     * A factory for {@linkplain FsModel file system models}.
     * <p>
     * Implementations should be safe for multi-threaded access.
     *
     * @param  <Context> The type of the calling context.
     * @author Christian Schlichtherle
     */
    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    interface Factory<Context> {

        /**
         * Returns a new file system model for the given mount point.
         * This is a pure function without side effects.
         * <p>
         * When called, you may assert the following precondition:
         * {@code assert mountPoint.getParent().equals(parent.map(FsModel::getMountPoint));}
         *
         * @param  context the calling context.
         * @param  mountPoint the mount point of the file system.
         * @param  parent the nullable parent file system model.
         * @return A new file system model for the given mount point.
         */
        FsModel newModel(
                Context context,
                FsMountPoint mountPoint,
                Optional<? extends FsModel> parent);
    }
}
