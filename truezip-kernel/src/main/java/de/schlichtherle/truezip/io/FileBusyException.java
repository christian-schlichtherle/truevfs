/*
 * Copyright (C) 2004-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.io;

import java.io.FileNotFoundException;
import javax.annotation.CheckForNull;
import javax.annotation.concurrent.ThreadSafe;

/**
 * Indicates that a file system entry could not get read or written
 * because the entry or its container is busy.
 * This exception is recoverable, meaning it should be possible to repeat the
 * operation successfully as soon as the entry or its container is not busy
 * anymore and unless no other exceptional condition applies.
 *
 * @author Christian Schlichtherle
 */
@ThreadSafe
public class FileBusyException extends FileNotFoundException {
    private static final long serialVersionUID = 2056108562576389242L;

    public FileBusyException(@CheckForNull String message) {
        super(message);
    }

    public FileBusyException(@CheckForNull Throwable cause) {
        super(null != cause ? cause.toString() : null);
        super.initCause(cause);
    }
}
