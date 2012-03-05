/*
 * Copyright (C) 2004-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.fs;

import de.schlichtherle.truezip.io.BusyIOException;
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
public final class FsResourceBusyIOException extends BusyIOException {
    private static final long serialVersionUID = 1L;

    FsResourceBusyIOException(int total, int local) {
        super("Total (thread local) number of unclosed archive entry resources: %d (%d)",
                total, local);
    }
}
