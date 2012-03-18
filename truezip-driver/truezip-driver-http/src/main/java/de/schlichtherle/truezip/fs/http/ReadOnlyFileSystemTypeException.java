/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package de.schlichtherle.truezip.fs.http;

import java.io.IOException;
import javax.annotation.concurrent.ThreadSafe;

/**
 * Thrown to indicate that an operation was trying to modify a read-only
 * file system.
 * 
 * @author  Christian Schlichtherle
 * @version $Id$
 */
@ThreadSafe
public final class ReadOnlyFileSystemTypeException extends IOException {

    private static final long serialVersionUID = 987645923512463262L;

    ReadOnlyFileSystemTypeException() {
        super("This file system type is read-only!");
    }
}
