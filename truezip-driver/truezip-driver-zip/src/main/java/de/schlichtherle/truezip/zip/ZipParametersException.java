/*
 * Copyright (C) 2011 Schlichtherle IT Services
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package de.schlichtherle.truezip.zip;

import java.util.zip.ZipException;
import net.jcip.annotations.ThreadSafe;

/**
 * Thrown to indicate that no suitable ZIP parameters have been provided
 * or something is wrong with these parameters.
 *
 * @since   TrueZIP 7.3
 * @author  Christian Schlichtherle
 * @version $Id$
 */
@ThreadSafe
public class ZipParametersException extends ZipException {
    private static final long serialVersionUID = 2032776586423467951L;

    /**
     * Constructs a ZIP parameters exception with
     * the given detail message.
     *
     * @param msg the detail message.
     */
    public ZipParametersException(String msg) {
        super(msg);
    }

    /**
     * Constructs a ZIP parameters exception with
     * the given cause.
     *
     * @param cause the cause for this exception to get thrown.
     */
    public ZipParametersException(Throwable cause) {
        super.initCause(cause);
    }
}
