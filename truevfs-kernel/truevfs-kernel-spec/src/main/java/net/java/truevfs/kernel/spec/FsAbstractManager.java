/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.kernel.spec;

import java.io.IOException;
import java.util.Objects;
import javax.annotation.concurrent.ThreadSafe;
import javax.inject.Provider;
import net.java.truecommons.shed.AbstractExceptionBuilder;
import net.java.truecommons.shed.BitField;
import net.java.truecommons.shed.Filter;
import net.java.truecommons.shed.UniqueObject;
import static net.java.truevfs.kernel.spec.FsSyncOption.ABORT_CHANGES;

/**
 * An abstract file system manager.
 * <p>
 * Subclasses should be thread-safe.
 *
 * @author Christian Schlichtherle
 */
@ThreadSafe
public abstract class FsAbstractManager
extends UniqueObject implements FsManager {

    @Override
    public void sync(
            BitField<FsSyncOption> options,
            Filter<? super FsController> filter)
    throws FsSyncWarningException, FsSyncException {
        sync(new DefaultSyncVisitor(options, filter));
    }

    private void sync(final SyncVisitor visitor)
    throws FsSyncException {
        if (visitor.getOptions().get(ABORT_CHANGES))
            throw new IllegalArgumentException();
        visit(visitor);
    }

    private <X extends IOException> void visit(
            final ControllerVisitor<X> visitor)
    throws X {
        try (final FsControllerStream stream = controllers(visitor)) {
            final AbstractExceptionBuilder<X, X> builder = visitor.get();
            for (final FsController controller : stream) {
                try {
                    visitor.visit(controller);
                } catch (final IOException ex) {
                    builder.warn((X) ex);
                }
            }
            builder.check();
        }
    }

    private static class DefaultSyncVisitor
    extends SyncVisitor {
        private final BitField<FsSyncOption> options;
        private final Filter<? super FsController> filter;

        public DefaultSyncVisitor() {
            this(FsSyncOptions.SYNC, Filter.ACCEPT_ANY);
        }

        public DefaultSyncVisitor(
                final BitField<FsSyncOption> options) {
            this(options, Filter.ACCEPT_ANY);
        }

        public DefaultSyncVisitor(
                final BitField<FsSyncOption> options,
                final Filter<? super FsController> filter) {
            this.options = Objects.requireNonNull(options);
            this.filter = Objects.requireNonNull(filter);
        }

        @Override
        public boolean accept(FsController controller) {
            return filter.accept(controller);
        }

        @Override
        public BitField<FsSyncOption> getOptions() {
            return options;
        }
    }

    private static abstract class SyncVisitor
    implements ControllerVisitor<FsSyncException> {

        @Override
        public AbstractExceptionBuilder<FsSyncException, FsSyncException> get() {
            return new FsSyncExceptionBuilder();
        }

        @Override
        public void visit(FsController controller) throws FsSyncException {
            controller.sync(getOptions());
        }

        public abstract BitField<FsSyncOption> getOptions();
    }

    private interface ControllerVisitor<X extends IOException>
    extends Filter<FsController>, Provider<AbstractExceptionBuilder<X, X>> {
        void visit(FsController controller) throws X;
    }

    /**
     * Returns a string representation of this object for debugging and logging
     * purposes.
     */
    @Override
    public String toString() {
        return String.format("%s@%x",
                getClass().getName(),
                hashCode());
    }
}
