/*
 * Copyright 2004-2012 Schlichtherle IT Services
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package de.schlichtherle.truezip.fs.archive;

import de.schlichtherle.truezip.fs.FsEntryName;
import javax.annotation.CheckForNull;
import java.io.IOException;
import javax.annotation.concurrent.ThreadSafe;

/**
 * Thrown to indicate an exceptional condition in an {@link FsArchiveFileSystem}.
 */
@ThreadSafe
public class FsArchiveFileSystemException extends IOException {
    private static final long serialVersionUID = 4652084652223428651L;

    /** The nullable entry path name. */
    private final @CheckForNull String path;

    /** @since TrueZIP 7.5 */
    FsArchiveFileSystemException(FsEntryName name, String message) {
        this(name.toString(), message);
    }

    FsArchiveFileSystemException(@CheckForNull String path, String message) {
        super(message);
        this.path = path;
    }

    FsArchiveFileSystemException(String path, IOException cause) {
        super(cause);
        this.path = path;
    }

    FsArchiveFileSystemException(@CheckForNull String path, String message, IOException cause) {
        super(message, cause);
        this.path = path;
    }

    @Override
    public String getMessage() {
        final String m = super.getMessage();
        return null != path ?
                new StringBuilder(path)
                    .append(" (")
                    .append(m)
                    .append(")")
                    .toString()
                : m;
    }
}
