/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.key;

import javax.annotation.CheckForNull;
import java.security.GeneralSecurityException;
import javax.annotation.concurrent.ThreadSafe;

/**
 * Thrown to indicate that the retrieval of the key to open or create a
 * protected resource has failed.
 * The subclass provides more information.
 *
 * @author Christian Schlichtherle
 */
@ThreadSafe
public class UnknownKeyException extends GeneralSecurityException {
    private static final long serialVersionUID = 6092786348232837265L;

    UnknownKeyException() {
    }

    protected UnknownKeyException(@CheckForNull String msg) {
        super(msg);
    }

    public UnknownKeyException(@CheckForNull Throwable cause) {
        super(cause);
    }
}