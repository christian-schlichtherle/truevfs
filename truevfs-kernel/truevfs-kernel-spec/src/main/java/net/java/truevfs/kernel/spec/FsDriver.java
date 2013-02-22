/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
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
implements FsModelFactory<FsManager>, FsControllerFactory<FsManager> {

    @Override
    public final FsModel newModel(
            FsManager context,
            FsMountPoint mountPoint,
            FsModel parent) {
        return context.newModel(this, mountPoint, parent);
    }

    /**
     * Returns {@code true} iff this is an archive driver, i.e. if file systems
     * of this type must be a member of a parent file system.
     * <p>
     * The implementation in the class {@link FsDriver} returns {@code false}.
     *
     * @return {@code true} iff this is an archive driver, i.e. if file systems
     *         of this type must be a member of a parent file system.
     */
    public boolean isArchiveDriver() { return false; }

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
