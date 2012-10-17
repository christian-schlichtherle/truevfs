/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.kernel.spec;

import java.io.IOException;
import javax.annotation.concurrent.ThreadSafe;
import net.java.truecommons.shed.ExceptionBuilder;
import net.java.truecommons.shed.Filter;
import net.java.truecommons.shed.UniqueObject;

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
    public void sync(FsControllerSyncVisitor visitor) throws FsSyncException {
        visit(visitor);
    }

    @Override
    public final <X extends IOException> void visit(
            final FsControllerVisitor<X> visitor)
    throws X {
        try (final FsControllerStream stream = stream(visitor.filter())) {
            final ExceptionBuilder<X, X> builder = visitor.builder();
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
    protected abstract FsControllerStream stream(Filter<? super FsController> filter);

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
