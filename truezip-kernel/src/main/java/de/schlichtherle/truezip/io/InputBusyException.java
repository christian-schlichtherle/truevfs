/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.io;

import javax.annotation.CheckForNull;
import javax.annotation.concurrent.ThreadSafe;

/**
 * Indicates that a file system entry could not get read
 * because the entry or its container is busy.
 * This exception is recoverable, meaning it should be possible to repeat the
 * operation successfully as soon as the entry or its container is not busy
 * anymore and unless no other exceptional condition applies.
 *
 * @see    OutputBusyException
 * @author Christian Schlichtherle
 */
@ThreadSafe
public class InputBusyException extends FileBusyException {
    private static final long serialVersionUID = 1983745618753823654L;

    public InputBusyException(@CheckForNull String message) {
        super(message);
    }

    public InputBusyException(@CheckForNull Throwable cause) {
        super(cause);
    }
}
