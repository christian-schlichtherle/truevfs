/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.kernel.spec;

import java.io.IOException;
import javax.annotation.concurrent.ThreadSafe;
import net.java.truecommons.shed.Filter;
import net.java.truecommons.shed.UniqueObject;
import net.java.truecommons.shed.Visitor;

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
            final Filter<? super FsController> filter,
            final Visitor<? super FsController, FsSyncException> visitor)
    throws FsSyncException {
        final FsSyncExceptionBuilder builder = new FsSyncExceptionBuilder();

        class AssembleExceptionVisitor
        implements Visitor<FsController, FsSyncException> {
            @Override
            public void visit(final FsController controller) {
                try {
                    visitor.visit(controller);
                } catch (final FsSyncException ex) {
                    builder.warn(ex);
                }
            }
        } // AssembleExceptionVisitor

        visit(filter, new AssembleExceptionVisitor());
        builder.check();
    }

    @Override
    public final <X extends IOException> void visit(
            final Filter<? super FsController> filter,
            final Visitor<? super FsController, X> visitor)
    throws X {
        try (final FsControllerStream s = stream(filter)) {
            for (final FsController c : s) visitor.visit(c);
        }
    }

    /**
     * Returns a file system controller stream which results from filtering
     * and sorting the managed file system controllers so that any child file
     * systems appear <em>before</em> their respective parent file system in
     * the stream.
     * This ensures that when calling {@link #sync}, all file system changes
     * have been processed upon successful termination.
     *
     * @param  filter the filter to apply to the managed file system
     *         controllers.
     * @return A filtered stream of managed file system controllers in reverse
     *         order of their {@link FsMountPoint} mount point.
     */
    protected abstract FsControllerStream stream(
            Filter<? super FsController> filter);

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
