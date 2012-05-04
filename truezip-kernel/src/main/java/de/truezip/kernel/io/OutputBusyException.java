/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.truezip.kernel.io;

import javax.annotation.CheckForNull;
import javax.annotation.concurrent.ThreadSafe;

/**
 * Indicates that a file system entry could not get written
 * because the entry or its container is busy.
 * This exception is recoverable, meaning it should be possible to repeat the
 * operation successfully as soon as the entry or its container is not busy
 * anymore and unless no other exceptional condition applies.
 *
 * @see    InputBusyException
 * @author Christian Schlichtherle
 */
@ThreadSafe
public class OutputBusyException extends FileBusyException {
    private static final long serialVersionUID = 962318648273654198L;
    
    public OutputBusyException(@CheckForNull String message) {
        super(message);
    }

    public OutputBusyException(@CheckForNull Throwable cause) {
        super(cause);
    }
}