/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.truezip.driver.zip.io;

import javax.annotation.CheckForNull;
import javax.annotation.concurrent.ThreadSafe;

/**
 * Thrown to indicate that an authenticated ZIP entry has been tampered with.
 *
 * @author  Christian Schlichtherle
 */
@ThreadSafe
public class ZipAuthenticationException extends ZipCryptoException {
    private static final long serialVersionUID = 2403462923846291232L;

    /**
     * Constructs a ZIP authentication exception with the given detail message.
     */
    public ZipAuthenticationException(@CheckForNull String msg) {
        super(msg);
    }
}