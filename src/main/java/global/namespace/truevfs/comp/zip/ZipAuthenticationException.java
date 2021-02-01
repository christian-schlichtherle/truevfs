/*
 * Copyright © 2005 - 2021 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package global.namespace.truevfs.comp.zip;

import javax.annotation.CheckForNull;

/**
 * Thrown to indicate that an authenticated ZIP entry has been tampered with.
 *
 * @author  Christian Schlichtherle
 */
public class ZipAuthenticationException extends ZipCryptoException {

    private static final long serialVersionUID = 0;

    /**
     * Constructs a ZIP authentication exception with the given detail message.
     */
    public ZipAuthenticationException(@CheckForNull String msg) {
        super(msg);
    }
}