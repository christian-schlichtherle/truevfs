/*
 * Copyright (C) 2011 Schlichtherle IT Services
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package de.schlichtherle.truezip.zip;

import net.jcip.annotations.ThreadSafe;

/**
 * Thrown to indicate that retrieving a key to encrypt or decrypt or
 * authenticate a ZIP entry has failed for some reason.
 *
 * @author Christian Schlichtherle
 * @version $Id$
 */
@ThreadSafe
public class ZipKeyException extends ZipParametersException {
    private static final long serialVersionUID = 5762312735142938698L;
    
    /**
     * Creates a ZIP key exception with
     * the given detail message.
     * 
     * @param msg the detail message.
     */
    public ZipKeyException(String msg) {
        super(msg);
    }

    /**
     * Creates a ZIP key exception with
     * the given cause.
     * 
     * @param cause the cause for this exception to get thrown.
     */
    public ZipKeyException(Throwable cause) {
        super(cause);
    }
}
