/*
 * Copyright (C) 2005-2011 Schlichtherle IT Services
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package de.schlichtherle.truezip.fs.archive;

import java.io.IOException;
import net.jcip.annotations.ThreadSafe;

/**
 * Thrown to indicate an exceptional condition in an {@link FsArchiveFileSystem}.
 */
@ThreadSafe
public class FsArchiveFileSystemException extends IOException {

    private static final long serialVersionUID = 4652084652223428651L;

    /** The entry's path name. */
    private final String path;

    FsArchiveFileSystemException(String path, String message) {
        super(message);
        this.path = path;
    }

    FsArchiveFileSystemException(String path, IOException cause) {
        super(cause != null ? cause.toString() : null);
        this.path = path;
        super.initCause(cause);
    }

    FsArchiveFileSystemException(String path, String message, IOException cause) {
        super(message);
        this.path = path;
        super.initCause(cause);
    }

    @Override
    public String getLocalizedMessage() {
        if (path != null)
            return new StringBuilder(path).append(" (").append(getMessage()).append(")").toString();
        return getMessage();
    }
}
