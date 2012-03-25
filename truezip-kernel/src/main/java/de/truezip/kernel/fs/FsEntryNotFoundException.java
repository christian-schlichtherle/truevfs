/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.truezip.kernel.fs;

import de.truezip.kernel.fs.addr.FsEntryName;
import de.truezip.kernel.fs.addr.FsPath;
import java.io.FileNotFoundException;
import java.io.IOException;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;

/**
 * Indicates that a file system entry does not exist or is not accessible.
 * 
 * @author Christian Schlichtherle
 */
@ThreadSafe
public final class FsEntryNotFoundException extends FileNotFoundException {
    private static final long serialVersionUID = 2972350932856838564L;

    private final FsPath path;

    FsEntryNotFoundException(
            final FsModel model,
            final FsEntryName name,
            final @CheckForNull String msg) {
        super(msg);
        this.path = model.getMountPoint().resolve(name);
    }

    FsEntryNotFoundException(
            final FsModel model,
            final FsEntryName name,
            final @CheckForNull IOException cause) {
        super(null != cause ? cause.toString() : null);
        super.initCause(cause);
        this.path = model.getMountPoint().resolve(name);
    }

    @Override
    public @Nullable String getMessage() {
        final String msg = super.getMessage();
        return null != msg
                ? new StringBuilder(path.toString()).append(" (").append(msg).append(")").toString()
                : path.toString();
    }
}
