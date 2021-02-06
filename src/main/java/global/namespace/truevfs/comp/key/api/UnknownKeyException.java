/*
 * Copyright (C) 2005-2015 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package global.namespace.truevfs.comp.key.api;

import javax.annotation.Nullable;
import java.security.GeneralSecurityException;

/**
 * Thrown to indicate that the retrieval of the key to (over)write or read a
 * protected resource has failed.
 *
 * @author Christian Schlichtherle
 */
public class UnknownKeyException extends GeneralSecurityException {

    private static final long serialVersionUID = 0;

    public UnknownKeyException() { }

    public UnknownKeyException(@Nullable String msg) { super(msg); }

    public UnknownKeyException(@Nullable Throwable cause) { super(cause); }

    public UnknownKeyException(@Nullable String msg, @Nullable Throwable cause) {
        super(msg, cause);
    }
}
