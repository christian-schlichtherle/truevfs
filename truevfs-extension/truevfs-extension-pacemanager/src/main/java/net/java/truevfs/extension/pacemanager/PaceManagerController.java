/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.extension.pacemanager;

import javax.annotation.concurrent.Immutable;
import net.java.truecommons.shed.BitField;
import net.java.truevfs.kernel.spec.*;

/**
 * The pace manager controller.
 * 
 * @author Christian Schlichtherle
 */
@Immutable
final class PaceManagerController extends FsDecoratingManager<FsManager> {

    private final PaceManagerModel model;

    PaceManagerController(final PaceManagerModel model, final FsManager manager) {
        super(manager);
        model.init(manager);
        this.model = model;
    }

    @Override
    public FsController<? extends FsModel> controller(FsCompositeDriver d, FsMountPoint mp) {
        return new PaceController(this, (FsController<FsModel>) manager.controller(d, mp));
    }

    /**
     * If the number of mounted archive files exceeds {@link #getMaximumFileSystemsMounted()},
     * then this method sync()s the least recently used (LRU) archive files
     * which exceed this value.
     * 
     * @param  controller the controller for the file system to retain mounted
     *         for subsequent access.
     * @throws FsSyncException 
     */
    void retain(FsController<? extends FsModel> controller) throws FsSyncException {
        model.retain(controller);
    }

    /**
     * Registers the archive file system of the given controller as the most
     * recently used (MRU).
     * 
     * @param  controller the controller for the most recently used file system.
     */
    void accessed(FsController<? extends FsModel> controller) {
        model.accessed(controller);
    }

    @Override
    public void sync(BitField<FsSyncOption> options) throws FsSyncException {
        model.sync(options);
    }
}
