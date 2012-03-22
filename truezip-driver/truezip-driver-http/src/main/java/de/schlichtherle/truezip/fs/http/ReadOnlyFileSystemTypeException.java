/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.fs.http;

import java.io.IOException;
import javax.annotation.concurrent.ThreadSafe;

/**
 * Thrown to indicate that an operation was trying to modify a read-only
 * file system.
 * 
 * @author  Christian Schlichtherle
 */
@ThreadSafe
public final class ReadOnlyFileSystemTypeException extends IOException {

    private static final long serialVersionUID = 987645923512463262L;

    ReadOnlyFileSystemTypeException() {
        super("This file system type is read-only!");
    }
}