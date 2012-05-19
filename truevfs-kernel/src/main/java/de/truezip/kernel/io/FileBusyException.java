/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.truezip.kernel.io;

import java.io.FileNotFoundException;
import javax.annotation.CheckForNull;
import javax.annotation.concurrent.ThreadSafe;

/**
 * Indicates that a file system entry could not get read or written
 * because the entry or its container is busy.
 * This exception should be recoverable, meaning it should be possible to
 * successfully retry the operation as soon as the resource is not busy anymore
 * and no other exceptional conditions apply.
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
        super(null == cause ? null : cause.toString());
        super.initCause(cause);
    }
}
