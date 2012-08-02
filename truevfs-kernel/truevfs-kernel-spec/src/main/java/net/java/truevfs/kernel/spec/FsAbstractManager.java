/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.kernel.spec;

import javax.annotation.concurrent.ThreadSafe;
import static net.java.truevfs.kernel.spec.FsSyncOption.ABORT_CHANGES;
import net.java.truecommons.shed.BitField;

/**
 * An abstract file system manager.
 *
 * @author Christian Schlichtherle
 */
@ThreadSafe
public abstract class FsAbstractManager
implements FsManager {

    @Override
    public void sync(final BitField<FsSyncOption> options)
    throws FsSyncWarningException, FsSyncException {
        if (options.get(ABORT_CHANGES)) throw new IllegalArgumentException();
        final FsSyncExceptionBuilder builder = new FsSyncExceptionBuilder();
        for (final FsController<?> controller : this) {
            try {
                controller.sync(options);
            } catch (final FsSyncException ex) {
                builder.warn(ex);
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
        return String.format("%s[size=%d]",
                getClass().getName(),
                size());
    }
}