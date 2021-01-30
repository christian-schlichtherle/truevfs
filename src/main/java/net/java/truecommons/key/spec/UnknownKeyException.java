/*
 * Copyright (C) 2005-2015 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truecommons.key.spec;

import javax.annotation.Nullable;
import java.security.GeneralSecurityException;

/**
 * Thrown to indicate that the retrieval of the key to (over)write or read a
 * protected resource has failed.
 *
 * @since  TrueCommons 2.2
 * @author Christian Schlichtherle
 */
public class UnknownKeyException extends GeneralSecurityException {

    private static final long serialVersionUID = 6092786348232837265L;

    public UnknownKeyException() { }

    public UnknownKeyException(@Nullable String msg) { super(msg); }

    public UnknownKeyException(@Nullable Throwable cause) { super(cause); }

    public UnknownKeyException(@Nullable String msg, @Nullable Throwable cause) {
        super(msg, cause);
    }
}
