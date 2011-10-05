/*
 * Copyright (C) 2004-2011 Schlichtherle IT Services
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package de.schlichtherle.truezip.fs;

import java.io.FileNotFoundException;
import java.io.IOException;
import net.jcip.annotations.ThreadSafe;

/**
 * Indicates that a file system entry does not exist or is not accessible.
 * 
 * @author Christian Schlichtherle
 * @version $Id$
 */
@ThreadSafe
public final class FsEntryNotFoundException extends FileNotFoundException {
    private static final long serialVersionUID = 2972350932856838564L;

    private final FsPath path;

    public FsEntryNotFoundException(
            final FsModel model,
            final FsEntryName name,
            final String msg) {
        super(msg);
        this.path = model.getMountPoint().resolve(name);
    }

    public FsEntryNotFoundException(
            final FsModel model,
            final FsEntryName name,
            final IOException cause) {
        super(cause == null ? null : cause.toString());
        super.initCause(cause);
        this.path = model.getMountPoint().resolve(name);
    }

    @Override
    public String getMessage() {
        final String msg = super.getMessage();
        return msg != null
                ? new StringBuilder(path.toString()).append(" (").append(msg).append(")").toString()
                : path.toString();
    }
}
