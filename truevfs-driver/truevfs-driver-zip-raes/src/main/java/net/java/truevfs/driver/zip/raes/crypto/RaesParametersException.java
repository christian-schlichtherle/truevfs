/*
 * Copyright © 2005 - 2021 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.driver.zip.raes.crypto;

import javax.annotation.CheckForNull;
import javax.annotation.concurrent.ThreadSafe;

/**
 * Thrown to indicate that no suitable cryptographic parameters have been
 * provided or something is wrong with these parameters.
 *
 * @author Christian Schlichtherle
 */
@ThreadSafe
public class RaesParametersException extends RaesException {
    private static final long serialVersionUID = 1605398165986459281L;

    /**
     * Constructs a RAES parameters exception with
     * the given detail message.
     *
     * @param msg the detail message.
     */
    public RaesParametersException(String msg) {
        super(msg);
    }

    /**
     * Constructs a RAES parameters exception with
     * the given cause.
     *
     * @param cause the cause for this exception to get thrown.
     */
    public RaesParametersException(Throwable cause) {
        super(cause);
    }
}
