/*
 * Copyright © 2005 - 2021 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package global.namespace.truevfs.kernel.impl;

import global.namespace.truevfs.commons.shed.BitField;
import global.namespace.truevfs.kernel.api.*;

import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * @author Christian Schlichtherle
 */
abstract class ArchiveModel<E extends FsArchiveEntry>
        extends FsDecoratingModel
        implements ReentrantReadWriteLockAspect {

    final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    private final FsArchiveDriver<E> driver;

    ArchiveModel(final FsArchiveDriver<E> driver, final FsModel model) {
        super(model);
        this.driver = driver;
    }

    final FsArchiveDriver<E> getDriver() {
        return driver;
    }

    @Override
    public ReentrantReadWriteLock getLock() {
        return lock;
    }

    /**
     * Composes the node path from the mountpoint of this model and the given node name.
     *
     * @param name the node name.
     */
    final FsNodePath path(FsNodeName name) {
        return new FsNodePath(Optional.of(getMountPoint()), name);
    }

    abstract void touch(BitField<FsAccessOption> options) throws IOException;
}
