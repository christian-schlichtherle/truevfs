/*
 * Copyright 2004-2012 Schlichtherle IT Services
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package de.schlichtherle.truezip.io;

/**
 * Indicates that a file system entry could not get written
 * because the entry or its container is busy.
 * This exception is recoverable, meaning it should be possible to repeat the
 * operation successfully as soon as the entry or its container is not busy
 * anymore and unless no other exceptional condition applies.
 *
 * @see     InputBusyException
 * @author  Christian Schlichtherle
 * @version $Id$
 */
public class OutputBusyException extends FileBusyException {
    private static final long serialVersionUID = 962318648273654198L;
    
    public OutputBusyException(String message) {
        super(message);
    }

    public OutputBusyException(Exception cause) {
        super(cause);
    }
}
