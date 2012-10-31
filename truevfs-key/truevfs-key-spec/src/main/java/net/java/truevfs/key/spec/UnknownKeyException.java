/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.key.spec;

import java.security.GeneralSecurityException;
import javax.annotation.CheckForNull;

/**
 * Thrown to indicate that the retrieval of the key to open or create a
 * protected resource has failed.
 * The subclass provides more information.
 *
 * @author Christian Schlichtherle
 */
public class UnknownKeyException extends GeneralSecurityException {

    private static final long serialVersionUID = 6092786348232837265L;

    public UnknownKeyException() { }

    public UnknownKeyException(@CheckForNull String msg) { super(msg); }

    public UnknownKeyException(@CheckForNull Throwable cause) { super(cause); }

    public UnknownKeyException(@CheckForNull String msg, @CheckForNull Throwable cause) {
        super(msg, cause);
    }
}
