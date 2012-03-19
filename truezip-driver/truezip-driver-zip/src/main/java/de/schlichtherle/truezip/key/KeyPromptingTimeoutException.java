/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.key;

import javax.annotation.CheckForNull;
import javax.annotation.concurrent.ThreadSafe;

/**
 * Thrown to indicate that prompting for a key to open or create a
 * protected resource has timed out.
 *
 * @author Christian Schlichtherle
 */
@ThreadSafe
public class KeyPromptingTimeoutException extends UnknownKeyException {
    private static final long serialVersionUID = 7656348612765052586L;

    public KeyPromptingTimeoutException() {
        super("Key prompting has timed out!");
    }

    public KeyPromptingTimeoutException(@CheckForNull Throwable cause) {
        super("Key prompting has timed out!");
        super.initCause(cause);
    }
}