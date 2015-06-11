/*
 * Copyright (C) 2005-2015 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.kernel.spec;

import net.java.truecommons.shed.BitField;
import net.java.truecommons.shed.Filter;
import net.java.truecommons.shed.Visitor;
import net.java.truevfs.kernel.spec.sl.FsManagerLocator;

import javax.inject.Provider;

import static java.util.Objects.requireNonNull;

/**
 * @author Christian Schlichtherle
 */
public final class FsSync {

    private Provider<FsManager> managerProvider = FsManagerLocator.SINGLETON;
    private Filter<? super FsController> filter = Filter.ACCEPT_ANY;
    private BitField<FsSyncOption> options = FsSyncOptions.NONE;

    public FsSync manager(final FsManager manager) {
        requireNonNull(manager);
        this.managerProvider = new Provider<FsManager>() {

            @Override
            public FsManager get() { return manager; }
        };
        return this;
    }

    public FsSync filter(final Filter<? super FsController> filter) {
        this.filter = requireNonNull(filter);
        return this;
    }

    public FsSync options(final BitField<FsSyncOption> options) {
        this.options = requireNonNull(options);
        return this;
    }

    /**
     * Invokes {@link FsController#sync sync()} on all managed file system
     * controllers which are accepted by the configured file system controller
     * filter.
     * <p>
     * The implementation uses an {@link FsSyncExceptionBuilder} while iterating
     * over all managed file system controllers in order to ensure that all
     * controllers get synced, even if a controller fails with an
     * {@link FsSyncException}.
     *
     * @throws FsSyncWarningException if <em>only</em> warning conditions
     *         apply.
     *         This implies that the respective file system controller has been
     *         {@link FsController#sync sync()}ed with constraints, e.g. if an
     *         open archive entry stream or channel gets forcibly closed.
     * @throws FsSyncException if any error conditions apply.
     */
    public void run() throws FsSyncException {

        final class SyncVisitor implements Visitor<FsController, RuntimeException> {

            final FsSyncExceptionBuilder builder = new FsSyncExceptionBuilder();

            @Override
            public void visit(final FsController controller) {
                try {
                    controller.sync(options);
                } catch (FsSyncException e) {
                    builder.warn(e);
                }
            }

            void check() throws FsSyncException {
                builder.check();
            }
        }

        manager().accept(filter, new SyncVisitor()).check();
    }

    private FsManager manager() { return managerProvider.get(); }
}
