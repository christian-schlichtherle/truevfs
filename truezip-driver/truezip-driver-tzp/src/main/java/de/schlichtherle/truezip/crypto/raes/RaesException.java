/*
 * Copyright 2004-2012 Schlichtherle IT Services
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package de.schlichtherle.truezip.crypto.raes;

import javax.annotation.CheckForNull;
import java.io.IOException;
import javax.annotation.concurrent.ThreadSafe;

/**
 * Thrown if there is an issue when reading or writing a RAES file which is
 * specific to the RAES file format.
 *
 * @author  Christian Schlichtherle
 * @version $Id$
 */
@ThreadSafe
public class RaesException extends IOException {
    private static final long serialVersionUID = 8564203786508562247L;

    /**
     * Constructs a RAES exception with
     * no detail message.
     */
    public RaesException() {
    }

    /**
     * Constructs a RAES exception with
     * the given detail message.
     *
     * @param msg the detail message.
     */
    public RaesException(@CheckForNull String msg) {
        super(msg);
    }

    /**
     * Constructs a RAES exception with
     * the given detail message and cause.
     *
     * @param msg the detail message.
     * @param cause the cause for this exception to be thrown.
     */
    public RaesException(@CheckForNull String msg, @CheckForNull Throwable cause) {
        super(msg, cause);
    }

    /**
     * Constructs a RAES exception with
     * the given cause.
     *
     * @param cause the cause for this exception to get thrown.
     */
    public RaesException(@CheckForNull Throwable cause) {
        super(cause);
    }
}
