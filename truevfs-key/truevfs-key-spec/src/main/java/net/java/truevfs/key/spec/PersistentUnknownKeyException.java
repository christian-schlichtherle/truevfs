/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.key.spec;

import javax.annotation.CheckForNull;
import javax.annotation.concurrent.ThreadSafe;

/**
 * Thrown to indicate that the retrieval of the key to open or create a
 * protected resource has failed and that this exception is cacheable.
 * The subclass provides more information.
 *
 * @author  Christian Schlichtherle
 */
public class PersistentUnknownKeyException extends UnknownKeyException {

    private static final long serialVersionUID = 2463586348235337265L;

    public PersistentUnknownKeyException() { }

    public PersistentUnknownKeyException(@CheckForNull String msg) { super(msg); }

    public PersistentUnknownKeyException(@CheckForNull Throwable cause) { super(cause); }

    public PersistentUnknownKeyException(@CheckForNull String msg, @CheckForNull Throwable cause) {
        super(msg, cause);
    }
}
