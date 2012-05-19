/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.truevfs.driver.zip.raes.crypto;

import javax.annotation.concurrent.ThreadSafe;

/**
 * Thrown to indicate that a RAES file has been tampered with.
 *
 * @author  Christian Schlichtherle
 */
@ThreadSafe
public class RaesAuthenticationException extends RaesException {
    private static final long serialVersionUID = 2362389234686232732L;

    /**
     * Constructs a RAES exception with
     * a detail message indicating that a RAES file has been tampered with.
     */
    public RaesAuthenticationException() {
        super("Authenticated file content has been tampered with!");
    }
}