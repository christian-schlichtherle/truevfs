/*
 * Copyright © 2005 - 2021 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.comp.zip;

import javax.annotation.CheckForNull;
import javax.annotation.concurrent.ThreadSafe;

/**
 * Thrown to indicate that retrieving a key to encrypt or decrypt or
 * authenticate a ZIP entry has failed for some reason.
 *
 * @author Christian Schlichtherle
 */
@ThreadSafe
public class ZipKeyException extends ZipParametersException {

    private static final long serialVersionUID = 0;

    /**
     * Creates a ZIP key exception with
     * the given detail message.
     *
     * @param msg the detail message.
     */
    public ZipKeyException(@CheckForNull String msg) {
        super(msg);
    }

    /**
     * Creates a ZIP key exception with
     * the given cause.
     *
     * @param cause the cause for this exception to get thrown.
     */
    public ZipKeyException(@CheckForNull Throwable cause) {
        super(cause);
    }
}
