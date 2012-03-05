/*
 * Copyright (C) 2004-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.fs;

import de.schlichtherle.truezip.io.BusyIOException;
import javax.annotation.concurrent.ThreadSafe;

/**
 * @since   TrueZIP 7.5
 * @author  Christian Schlichtherle
 */
@ThreadSafe
public final class FsBusyIOException extends BusyIOException {
    private static final long serialVersionUID = 1L;

    FsBusyIOException(int total, int local) {
        super("Total (thread local) number of open I/O resources: %d (%d)",
                total, local);
    }
}
