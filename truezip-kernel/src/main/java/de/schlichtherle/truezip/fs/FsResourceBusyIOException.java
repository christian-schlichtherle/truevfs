/*
 * Copyright (C) 2004-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.fs;

import java.io.IOException;
import javax.annotation.concurrent.ThreadSafe;

/**
 * Indicates that a call to {@link FsController#sync} cannot succeed because
 * some threads have unclosed archive entry resources (e.g. streams).
 * <p>
 * This exception should be recoverable, meaning it should be possible to
 * successfully retry the operation as soon as these resources have been closed
 * and no other exceptional conditions apply.
 * 
 * @since   TrueZIP 7.5
 * @author  Christian Schlichtherle
 */
@ThreadSafe
public final class FsResourceBusyIOException extends IOException {
    private static final long serialVersionUID = 1L;

    final int total, local;

    FsResourceBusyIOException(int total, int local) {
        super("Total (thread local) number of unclosed archive entry resources: %d (%d)");
        this.total = total;
        this.local = local;
    }

    @Override
    public String getMessage() {
        return String.format(super.getMessage(), total, local);
    }
}
