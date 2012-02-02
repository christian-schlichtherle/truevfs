/*
 * Copyright 2004-2012 Schlichtherle IT Services
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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
