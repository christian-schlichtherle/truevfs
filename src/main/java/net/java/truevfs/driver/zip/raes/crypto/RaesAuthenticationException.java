/*
 * Copyright © 2005 - 2021 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.driver.zip.raes.crypto;

/**
 * Thrown to indicate that a RAES file has been tampered with.
 *
 * @author  Christian Schlichtherle
 */
public class RaesAuthenticationException extends RaesException {

    private static final long serialVersionUID = 0;

    /**
     * Constructs a RAES exception with
     * a detail message indicating that a RAES file has been tampered with.
     */
    public RaesAuthenticationException() {
        super("Authenticated file content has been tampered with!");
    }
}