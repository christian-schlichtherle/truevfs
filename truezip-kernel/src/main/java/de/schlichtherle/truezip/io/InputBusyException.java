/*
 * Copyright 2004-2012 Schlichtherle IT Services
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package de.schlichtherle.truezip.io;

import edu.umd.cs.findbugs.annotations.CheckForNull;
import net.jcip.annotations.ThreadSafe;

/**
 * Indicates that a file system entry could not get read
 * because the entry or its container is busy.
 * This exception is recoverable, meaning it should be possible to repeat the
 * operation successfully as soon as the entry or its container is not busy
 * anymore and unless no other exceptional condition applies.
 *
 * @see     OutputBusyException
 * @author  Christian Schlichtherle
 * @version $Id$
 */
@ThreadSafe
public class InputBusyException extends FileBusyException {
    private static final long serialVersionUID = 1983745618753823654L;

    public InputBusyException(@CheckForNull String message) {
        super(message);
    }

    public InputBusyException(@CheckForNull Throwable cause) {
        super(cause);
    }
}
