/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.kernel.spec;

import javax.annotation.concurrent.ThreadSafe;
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
            final BitField<FsSyncOption> options,
            final Filter<? super FsController> filter)
    throws FsSyncWarningException, FsSyncException {
        if (options.get(ABORT_CHANGES)) throw new IllegalArgumentException();
        final FsSyncExceptionBuilder builder = new FsSyncExceptionBuilder();
        try (final FsControllerStream stream = controllers(filter)) {
            for (final FsController controller : stream) {
                try {
                    controller.sync(options);
                } catch (final FsSyncException ex) {
                    builder.warn(ex);
                }
            }
        }
        builder.check();
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
