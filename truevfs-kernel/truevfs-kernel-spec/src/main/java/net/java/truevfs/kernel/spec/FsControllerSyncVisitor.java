/*
 * Copyright (C) 2005-2013 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.kernel.spec;

import java.util.Objects;
import javax.annotation.concurrent.ThreadSafe;
import net.java.truecommons.shed.BitField;
import net.java.truecommons.shed.Visitor;

/**
 * A visitor which performs a {@link FsController#sync} operation with the
 * options provided to its
 * {@linkplain #FsControllerSyncVisitor constructor} on any given file
 * system controller.
 * <p>
 * Subclasses should be thread-safe.
 *
 * @author Christian Schlichtherle
 */
@ThreadSafe
public final class FsControllerSyncVisitor
implements Visitor<FsController, FsSyncException> {

    private final BitField<FsSyncOption> options;

    /**
     * Constructs a controller sync visitor.
     *
     * @param options the options to use for
     *        {@linkplain FsController#sync sync}ing any given file system
     *        controller.
     */
    public FsControllerSyncVisitor(final BitField<FsSyncOption> options) {
        this.options = Objects.requireNonNull(options);
    }

    /**
     * Calls {@link FsController#sync} on the given controller with the options
     * provided to the {@linkplain #FsControllerSyncVisitor constructor}.
     *
     * @param  controller the file system controller to {@code sync()}.
     * @throws FsSyncException if {@code sync()}ing the file system failed for
     *         some reason.
     */
    @Override
    public void visit(FsController controller) throws FsSyncException {
        controller.sync(options);
    }
}
