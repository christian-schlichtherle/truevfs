/*
 * Copyright (C) 2005-2020 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.kernel.impl;

import net.java.truecommons.shed.BitField;
import net.java.truevfs.kernel.spec.*;

import java.io.IOException;
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
        return new FsNodePath(getMountPoint(), name);
    }

    abstract void touch(BitField<FsAccessOption> options) throws IOException;
}
