/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.truezip.kernel;

import javax.annotation.concurrent.ThreadSafe;

/**
 * Indicate that an operation was trying to modify a read-only (federated)
 * file system.
 */
@ThreadSafe
public final class FsReadOnlyFileSystemException extends FsFileSystemException {
    private static final long serialVersionUID = 987645923519873262L;

    public FsReadOnlyFileSystemException() {
        super((String) null, "This is a read-only file system!");
    }
}