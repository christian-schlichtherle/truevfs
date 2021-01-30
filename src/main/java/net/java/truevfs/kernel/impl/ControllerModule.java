/*
 * Copyright © 2005 - 2021 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.kernel.impl;

import bali.Cache;
import bali.Lookup;
import bali.Make;
import bali.Module;
import net.java.truecommons.cio.IoBufferPool;
import net.java.truevfs.kernel.spec.FsArchiveDriver;
import net.java.truevfs.kernel.spec.FsArchiveEntry;
import net.java.truevfs.kernel.spec.FsController;
import net.java.truevfs.kernel.spec.FsModel;

import static bali.CachingStrategy.NOT_THREAD_SAFE;

@Module
interface ControllerModule<E extends FsArchiveEntry> {

    @Lookup(param = "driver")
    FsArchiveDriver<E> getDriver();

    default FsController driverDecorate(FsController controller) {
        return getDriver().decorate(controller);
    }

    @Cache(NOT_THREAD_SAFE)
    default IoBufferPool getPool() {
        return getDriver().getPool();
    }

    @Make(ArchiveControllerAdapter.class)
    FsController newArchiveControllerAdapter(ArchiveController<E> controller);

    @Make(CacheController.class)
    ArchiveController<E> newCacheController(ArchiveController<E> controller);

    default FsController newControllerChain(FsModel model, FsController parent) {
        assert null != parent;
        assert model.getParent() == parent.getModel();
        assert !(model instanceof ArchiveModel);
        // HC SVNT DRACONES!
        // The FalsePositiveArchiveController decorates the FinalizeController so that it does not need to resolve
        // operations on false positive archive files.
        // The FinalizeController decorates the LockController so that any streams or channels referencing archive files
        // eligible for garbage collection get automatically closed.
        // The LockController decorates the SyncController so that the extended controller (chain) doesn't need to be thread
        // safe.
        // The SyncController decorates the CacheController because the selective entry cache needs to get flushed on a
        // NeedsSyncException.
        // The CacheController decorates the ResourceController because the cache entries terminate streams and channels and
        // shall not stop the extended controller (chain) from getting synced.
        // The ResourceController decorates the TargetArchiveController so that trying to sync the file system while any
        // stream or channel to the latter is open gets detected and properly dealt with.
        return newFalsePositiveArchiveController(
                newFinalizeController(
                        driverDecorate(
                                newArchiveControllerAdapter(
                                        newLockController(
                                                newSyncController(
                                                        newCacheController(
                                                                newResourceController(
                                                                        newTargetArchiveController(model, parent)))))))));
    }

    @Make(FalsePositiveArchiveController.class)
    FsController newFalsePositiveArchiveController(FsController controller);

    @Make(FinalizeController.class)
    FsController newFinalizeController(FsController controller);

    @Make(LockController.class)
    ArchiveController<E> newLockController(ArchiveController<E> controller);

    @Make(ResourceController.class)
    ArchiveController<E> newResourceController(ArchiveController<E> controller);

    @Make(SyncController.class)
    ArchiveController<E> newSyncController(ArchiveController<E> controller);

    @Make(TargetArchiveController.class)
    ArchiveController<E> newTargetArchiveController(FsModel model, FsController parent);
}
