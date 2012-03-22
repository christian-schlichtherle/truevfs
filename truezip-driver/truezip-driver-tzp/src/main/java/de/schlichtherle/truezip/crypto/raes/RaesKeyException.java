/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.crypto.raes;

import javax.annotation.CheckForNull;
import javax.annotation.concurrent.ThreadSafe;

/**
 * Thrown to indicate that retrieving a key to encrypt or decrypt some pay load
 * data in an RAES file has failed for some reason.
 *
 * @author  Christian Schlichtherle
 */
@ThreadSafe
public class RaesKeyException extends RaesParametersException {
    private static final long serialVersionUID = 1375629384612351398L;
    
    /**
     * Constructs a RAES key exception with
     * the given detail message.
     * 
     * @param msg the detail message.
     */
    public RaesKeyException(@CheckForNull String msg) {
        super(msg);
    }

    /**
     * Constructs a RAES key exception with
     * the given cause.
     * 
     * @param cause the cause for this exception to get thrown.
     */
    public RaesKeyException(@CheckForNull Throwable cause) {
        super(cause);
    }
}