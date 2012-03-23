/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.fs;

import de.schlichtherle.truezip.fs.addr.FsEntryName;
import java.io.IOException;
import javax.annotation.CheckForNull;
import javax.annotation.concurrent.ThreadSafe;

/**
 * Indicates that a (federated) file system operation failed for some reason.
 * 
 * @author Christian Schlichtherle
 */
@ThreadSafe
public class FsFileSystemException extends IOException {
    private static final long serialVersionUID = 4652084652223428651L;

    /** The nullable entry path name. */
    private final @CheckForNull String path;

    /** @since TrueZIP 7.5 */
    FsFileSystemException(FsEntryName name, String message) {
        this(name.toString(), message);
    }

    FsFileSystemException(@CheckForNull String path, String message) {
        super(message);
        this.path = path;
    }

    FsFileSystemException(@CheckForNull String path, Throwable cause) {
        super(cause);
        this.path = path;
    }

    FsFileSystemException(@CheckForNull String path, String message, Throwable cause) {
        super(message, cause);
        this.path = path;
    }

    @Override
    public String getMessage() {
        final String m = super.getMessage();
        return null == path
                ? m
                : new StringBuilder(path.isEmpty() ? "<file system root>" : path)
                    .append(" (")
                    .append(m)
                    .append(")")
                    .toString();
    }
}
