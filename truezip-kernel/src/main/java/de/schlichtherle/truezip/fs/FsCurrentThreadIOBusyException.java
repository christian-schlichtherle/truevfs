/*
 * Copyright (C) 2004-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.fs;

import javax.annotation.concurrent.ThreadSafe;

/**
 * @since   TrueZIP 7.5
 * @see     FsResourceController
 * @author  Christian Schlichtherle
 */
@ThreadSafe
public final class FsCurrentThreadIOBusyException
extends FsResourceIOBusyException {

    private static final long serialVersionUID = 1L;

    FsCurrentThreadIOBusyException(int local) {
        super("Number of open I/O resources for the current thread: %d", local);
    }
}
