/*
 * Copyright © 2005 - 2021 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.kernel.impl;

import lombok.val;
import net.java.truecommons.shed.Filter;
import net.java.truecommons.shed.Link;
import net.java.truecommons.shed.Visitor;
import net.java.truevfs.kernel.spec.*;

import javax.annotation.concurrent.ThreadSafe;
import java.util.*;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

import static net.java.truecommons.shed.Link.Type.STRONG;
import static net.java.truecommons.shed.Link.Type.WEAK;

/**
 * The default implementation of a file system manager.
 *
 * @author Christian Schlichtherle
 */
@ThreadSafe
final class DefaultManager extends FsAbstractManager implements ReentrantReadWriteLockAspect {

    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    /**
     * The map of all schedulers for composite file system controllers, keyed by the mount point of their respective
     * file system model.
     */
    private final Map<FsMountPoint, Link<FsController>> controllers = new WeakHashMap<>();

    private final ShutdownFuse syncOnShutdown = new ShutdownFuse(() -> {
        try {
            new FsSync().manager(this).options(FsSyncOptions.UMOUNT).run();
        } catch (FsSyncException e) {
            e.printStackTrace();
        }
    });

    @Override
    public ReentrantReadWriteLock getLock() {
        return lock;
    }

    @Override
    public FsModel newModel(FsDriver driver, FsMountPoint mountPoint, FsModel parent) {
        return driver.decorate(new DefaultModel(mountPoint, parent));
    }

    @Override
    public FsController newController(FsArchiveDriver<? extends FsArchiveEntry> driver, FsModel model, FsController parent) {
        return ControllerModuleFactory.INSTANCE.module(driver).newControllerChain(model, parent);
    }

    @Override
    public FsController controller(final FsCompositeDriver driver, final FsMountPoint mountPoint) {
        try {
            return readLocked(new Op<FsController, RuntimeException>() {

                @Override
                public FsController call() throws RuntimeException {
                    return controller0(driver, mountPoint);
                }
            });
        } catch (NeedsWriteLockException ex) {
            if (readLockedByCurrentThread()) {
                throw ex;
            }
            return writeLocked(new Op<FsController, RuntimeException>() {

                @Override
                public FsController call() throws RuntimeException {
                    return controller0(driver, mountPoint);
                }
            });
        }
    }

    private FsController controller0(final FsCompositeDriver driver, final FsMountPoint mountPoint) {
        val oc = Optional
                .ofNullable(controllers.get(mountPoint))
                .flatMap(l -> Optional.ofNullable(l.get()));
        if (oc.isPresent()) {
            return oc.get();
        } else {
            checkWriteLockedByCurrentThread();
            val opc = Optional.ofNullable(mountPoint.getParent()).map(y -> controller0(driver, y));
            val opm = opc.map(FsController::getModel);
            val m = new ManagedModel(driver.newModel(this, mountPoint, opm.orElse(null)));
            val c = driver.newController(this, m, opc.orElse(null));
            m.init(c);
            return c;
        }
    }

    @Override
    public <X extends Exception, V extends Visitor<? super FsController, X>> V accept(final Filter<? super FsController> filter, final V visitor) throws X {
        return new Op<V, X>() {

            boolean allUnmounted = true;

            @Override
            public V call() throws X {
                try {
                    for (val controller : readLocked(new Op<List<FsController>, RuntimeException>() {

                        @Override
                        public List<FsController> call() throws RuntimeException {
                            return controllers
                                    .values()
                                    .stream()
                                    .map(Link::get)
                                    .filter(Objects::nonNull)
                                    .filter(c -> {
                                        val accepted = filter.accept(c);
                                        allUnmounted &= accepted;
                                        return accepted;
                                    })
                                    .sorted(new FsControllerComparator())
                                    .collect(Collectors.toList());
                        }
                    })) {
                        try {
                            visitor.visit(controller);
                        } finally {
                            allUnmounted &= !controller.getModel().isMounted();
                        }
                    }
                } finally {
                    if (allUnmounted) {
                        syncOnShutdown.disarm();
                    }
                }
                return visitor;
            }
        }.call();
    }

    /**
     * A model which schedules its controller for synchronization by observing its property {@code mounted} - see method
     * {@code sync(BitField)}.
     */
    private final class ManagedModel extends FsDecoratingModel {

        FsController _controller;

        ManagedModel(FsModel model) {
            super(model);
        }

        void init(FsController controller) {
            assert null != controller;
            assert !model.isMounted();
            _controller = controller;
            schedule(false);
        }

        /**
         * Schedules the file system controller for synchronization according to the given mount status.
         */
        @Override
        public void setMounted(boolean mounted) {
            writeLocked(new Op<Void, RuntimeException>() {

                @Override
                public Void call() throws RuntimeException {
                    if (model.isMounted() != mounted) {
                        if (mounted) {
                            syncOnShutdown.arm();
                        }
                        ManagedModel.this.schedule(mounted);
                        model.setMounted(mounted);
                    }
                    return null;
                }
            });
        }

        void schedule(final boolean mandatory) {
            assert writeLockedByCurrentThread();
            controllers.put(getMountPoint(), (mandatory ? STRONG : WEAK).newLink(_controller));
        }
    }
}
