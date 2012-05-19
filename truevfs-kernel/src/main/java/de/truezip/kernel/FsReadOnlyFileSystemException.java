/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.truezip.kernel;

import java.nio.file.FileSystemException;
import javax.annotation.concurrent.ThreadSafe;

/**
 * Indicates that an I/O operation was trying to modify a read-only file system.
 */
@ThreadSafe
public class FsReadOnlyFileSystemException extends FileSystemException {
    private static final long serialVersionUID = 987645923519873262L;

    public FsReadOnlyFileSystemException() {
        super(null);
    }
}
