/*
 * Copyright © 2005 - 2021 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package global.namespace.truevfs.kernel.api;

import global.namespace.truevfs.comp.shed.UniqueObject;

import java.util.Objects;
import java.util.Optional;

import static java.util.Locale.ENGLISH;

/**
 * An abstract file system model which does <em>not</em> implement the property
 * {@code touched}.
 * <p>
 * Subclasses should be thread-safe.
 *
 * @author Christian Schlichtherle
 */
@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
public abstract class FsAbstractModel extends UniqueObject implements FsModel {

    private final FsMountPoint mountPoint;
    private final Optional<? extends FsModel> parent;

    protected FsAbstractModel(
            final FsMountPoint mountPoint,
            final Optional<? extends FsModel> parent) {
        if (!Objects.equals(mountPoint.getParent(), parent.map(FsModel::getMountPoint))) {
            throw new IllegalArgumentException("Parent/Member mismatch!");
        }
        this.mountPoint = mountPoint;
        this.parent = parent;
    }

    @Override
    public final FsMountPoint getMountPoint() {
        return mountPoint;
    }

    @Override
    public final Optional<? extends FsModel> getParent() {
        return parent;
    }

    /**
     * Returns a string representation of this object for debugging and logging
     * purposes.
     */
    @Override
    public String toString() {
        return String.format(ENGLISH,
                "%s@%x[mountPoint=%s, parent=%s, mounted=%b]",
                getClass().getName(),
                hashCode(),
                mountPoint,
                parent,
                isMounted());
    }
}
