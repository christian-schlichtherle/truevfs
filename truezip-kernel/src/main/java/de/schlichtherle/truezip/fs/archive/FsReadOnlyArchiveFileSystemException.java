/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.fs.archive;

import javax.annotation.concurrent.ThreadSafe;

/**
 * Thrown to indicate that an operation was trying to modify a read-only
 * {@link FsArchiveFileSystem}.
 */
@ThreadSafe
public final class FsReadOnlyArchiveFileSystemException
extends FsArchiveFileSystemException {

    private static final long serialVersionUID = 987645923519873262L;

    FsReadOnlyArchiveFileSystemException() {
        super((String) null, "This is a read-only archive file system!");
    }
}