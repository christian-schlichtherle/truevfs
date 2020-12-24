/*
 * Copyright (C) 2005-2020 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.kernel.impl;

import net.java.truecommons.shed.BitField;
import net.java.truevfs.kernel.spec.*;

import java.io.IOException;

/**
 * A generic mixin which provides some features of its associated {@link #getModel()}.
 *
 * @author Christian Schlichtherle
 */
interface ArchiveModelAspect<E extends FsArchiveEntry> {

    /**
     * Returns the archive model.
     */
    ArchiveModel<E> getModel();

    /**
     * Returns the mount point of the (federated virtual) file system.
     */
    default FsMountPoint getMountPoint() {
        return getModel().getMountPoint();
    }

    /**
     * Returns the `touched` property of the (federated virtual) file system.
     */
    default boolean isMounted() {
        return getModel().isMounted();
    }

    /**
     * Sets the `touched` property of the (federated virtual) file system.
     *
     * @param mounted the `mounted` property of the (federated virtual) file system.
     */
    default void setMounted(boolean mounted) {
        getModel().setMounted(mounted);
    }

    default FsArchiveDriver<E> getDriver() {
        return getModel().getDriver();
    }

    /**
     * Composes the node path from the mountpoint of this model and the given node name.
     *
     * @param name the node name.
     */
    default FsNodePath path(FsNodeName name) {
        return getModel().path(name);
    }

    default void touch(BitField<FsAccessOption> options) throws IOException {
        getModel().touch(options);
    }
}
