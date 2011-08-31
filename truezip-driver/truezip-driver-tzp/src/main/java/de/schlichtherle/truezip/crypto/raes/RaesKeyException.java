/*
 * Copyright (C) 2005-2011 Schlichtherle IT Services
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package de.schlichtherle.truezip.crypto.raes;

import net.jcip.annotations.ThreadSafe;

/**
 * Thrown to indicate that retrieving a key to encrypt or decrypt some pay load
 * data in an RAES file has failed for some reason.
 *
 * @author  Christian Schlichtherle
 * @version $Id$
 */
@ThreadSafe
public class RaesKeyException extends RaesParametersException {
    private static final long serialVersionUID = 1375629384612351398L;

    /**
     * Constructs a RAES key exception with
     * a detail message indicating that RAES key retrieval has failed.
     */
    public RaesKeyException() {
        super("RAES key retrieval has failed!");
    }
    
    /**
     * Constructs a RAES key exception with
     * the given detail message.
     * 
     * @param msg the detail message.
     */
    public RaesKeyException(String msg) {
        super(msg);
    }

    /**
     * Constructs a RAES key exception with
     * the given cause.
     * 
     * @param cause the cause for this exception to get thrown.
     */
    public RaesKeyException(Throwable cause) {
        super(cause);
    }
}
