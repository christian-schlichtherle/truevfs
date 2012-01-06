/*
 * Copyright 2004-2012 Schlichtherle IT Services
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package de.schlichtherle.truezip.io;

import java.io.FileNotFoundException;

/**
 * Indicates that a file system entry could not get read or written
 * because the entry or its container is busy.
 * This exception is recoverable, meaning it should be possible to repeat the
 * operation successfully as soon as the entry or its container is not busy
 * anymore and unless no other exceptional condition applies.
 *
 * @author Christian Schlichtherle
 * @version $Id$
 */
public class FileBusyException extends FileNotFoundException {
    private static final long serialVersionUID = 2056108562576389242L;

    FileBusyException(String message) {
        super(message);
    }

    FileBusyException(Exception cause) {
        super(null == cause ? null : cause.toString());
        super.initCause(cause);
    }
}
