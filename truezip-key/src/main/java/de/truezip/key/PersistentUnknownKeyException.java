/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.truezip.key;

import javax.annotation.CheckForNull;
import javax.annotation.concurrent.ThreadSafe;

/**
 * Thrown to indicate that the retrieval of the key to open or create a
 * protected resource has failed and that this exception is cacheable.
 * The subclass provides more information.
 *
 * @author  Christian Schlichtherle
 */
@ThreadSafe
public class PersistentUnknownKeyException extends UnknownKeyException {
    private static final long serialVersionUID = 2463586348235337265L;

    PersistentUnknownKeyException() {
    }

    PersistentUnknownKeyException(@CheckForNull String message) {
        super(message);
    }
}