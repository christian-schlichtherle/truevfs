/*
 * Copyright 2004-2012 Schlichtherle IT Services
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package de.schlichtherle.truezip.fs;

import net.jcip.annotations.Immutable;

/**
 * Indicates an <em>internal</em> exception in a decorator chain of
 * {@linkplain FsController file system controllers}.
 * <p>
 * Exceptions of this type are exclusively reserved for long distance flow
 * control in a decorator chain of file system controllers, e.g. to deal with
 * false positive archive files, require write locks, prevent dead locks,
 * automatically synchronize federated (archive) file systems etc.
 * File system controllers throw and catch these exceptions within their
 * decorator chain, so an exception of this type should never pop up to an
 * application!
 *
 * @see     FsController
 * @author  Christian Schlichtherle
 * @version $Id$
 */
@Immutable
@SuppressWarnings("serial") // serializing an exception for a temporary event is nonsense!
public abstract class FsControllerException extends RuntimeException {
    FsControllerException() {
        this(null);
    }

    FsControllerException(Throwable cause) {
        super(cause);
    }
}
