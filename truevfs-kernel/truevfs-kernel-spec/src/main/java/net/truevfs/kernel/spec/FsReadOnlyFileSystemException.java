/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.truevfs.kernel.spec;

import java.nio.file.FileSystemException;
import javax.annotation.concurrent.ThreadSafe;

/**
 * Indicates that an I/O operation was trying to modify a read-only file system.
 * <p>
 * The primary difference between this exception class and
 * {@link java.nio.file.ReadOnlyFileSystemException} is that this class is a
 * subclass of {@code FileSystemException} while the latter is a subclass of
 * {@link java.lang.UnsupportedOperationException}.
 */
@ThreadSafe
public class FsReadOnlyFileSystemException extends FileSystemException {
    private static final long serialVersionUID = 987645923519873262L;

    public FsReadOnlyFileSystemException() {
        super(null);
    }
}
