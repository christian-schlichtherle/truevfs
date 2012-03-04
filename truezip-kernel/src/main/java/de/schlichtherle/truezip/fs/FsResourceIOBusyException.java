/*
 * Copyright (C) 2004-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.fs;

import de.schlichtherle.truezip.io.FileBusyException;
import javax.annotation.concurrent.ThreadSafe;

/**
 * @since  TrueZIP 7.5
 * @see    FsResourceController
 * @author Christian Schlichtherle
 */
@ThreadSafe
public abstract class FsResourceIOBusyException extends FileBusyException {
    private static final long serialVersionUID = 1L;

    final Object[] args;

    FsResourceIOBusyException(final String message, final Object... args) {
        super(message);
        this.args = args;
    }

    @Override
    public String getMessage() {
        return String.format(super.getMessage(), args);
    }
}
