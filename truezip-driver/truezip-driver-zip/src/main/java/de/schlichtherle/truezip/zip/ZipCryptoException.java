/*
 * Copyright 2004-2012 Schlichtherle IT Services
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package de.schlichtherle.truezip.zip;

import javax.annotation.CheckForNull;
import java.util.zip.ZipException;
import javax.annotation.concurrent.ThreadSafe;

/**
 * Thrown if there is an issue when reading or writing an encrypted ZIP file
 * or entry.
 *
 * @since   TrueZIP 7.3
 * @author  Christian Schlichtherle
 * @version $Id$
 */
@ThreadSafe
public class ZipCryptoException extends ZipException {
    private static final long serialVersionUID = 2783693745683625471L;

    /**
     * Constructs a ZIP crypto exception with
     * no detail message.
     */
    public ZipCryptoException() {
    }

    /**
     * Constructs a ZIP crypto exception with
     * the given detail message.
     *
     * @param msg the detail message.
     */
    public ZipCryptoException(@CheckForNull String msg) {
        super(msg);
    }

    /**
     * Constructs a ZIP crypto exception with
     * the given detail message and cause.
     *
     * @param msg the detail message.
     * @param cause the cause for this exception to be thrown.
     */
    public ZipCryptoException(@CheckForNull String msg, @CheckForNull Throwable cause) {
        super(msg);
        super.initCause(cause);
    }

    /**
     * Constructs a ZIP crypto exception with
     * the given cause.
     *
     * @param cause the cause for this exception to get thrown.
     */
    public ZipCryptoException(@CheckForNull Throwable cause) {
        super(null != cause ? cause.toString() : null);
        super.initCause(cause);
    }
}
