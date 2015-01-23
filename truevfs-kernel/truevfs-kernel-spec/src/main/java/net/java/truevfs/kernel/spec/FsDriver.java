/*
 * Copyright (C) 2005-2015 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.kernel.spec;

import javax.annotation.concurrent.Immutable;
import net.java.truecommons.shed.UniqueObject;

/**
 * An abstract factory for components required to access a file system.
 * <p>
 * Subclasses should be immutable.
 *
 * @see    FsCompositeDriver
 * @author Christian Schlichtherle
 */
@Immutable
public abstract class FsDriver
extends UniqueObject
implements FsModel.Factory<FsManager>, FsController.Factory<FsManager> {

    /**
     * Returns {@code true} if and only if this is an archive driver, that is,
     * if file systems of this type must be a member of a parent file system.
     *
     * @return The implementation in the class {@link FsDriver}
     *         unconditionally returns {@code false}.
     */
    public boolean isArchiveDriver() { return false; }

    /**
     * {@inheritDoc}
     * <p>
     * The implementation in the class {@link FsDriver} forwards the call to
     * the given file system manager.
     */
    @Override
    public final FsModel newModel(
            FsManager context,
            FsMountPoint mountPoint,
            FsModel parent) {
        assert null == parent
                ? null == mountPoint.getParent()
                : parent.getMountPoint().equals(mountPoint.getParent());
        return context.newModel(this, mountPoint, parent);
    }

    /**
     * Decorates the given file system model.
     *
     * @param  model the file system model to decorate.
     * @return The implementation in the class {@link FsDriver}
     *         unconditionally returns {@code model}.
     */
    public FsModel decorate(FsModel model) { return model; }

    /**
     * Returns a string representation of this object for debugging and logging
     * purposes.
     */
    @Override
    public String toString() {
        return String.format("%s@%x[archiveDriver=%b]",
                getClass().getName(),
                hashCode(),
                isArchiveDriver());
    }
}
