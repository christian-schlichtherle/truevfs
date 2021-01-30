/*
 * Copyright (C) 2005-2015 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truecommons3.key.spec.prompting;

import net.java.truecommons3.key.spec.UnknownKeyException;

import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;

/**
 * Thrown to indicate that prompting for a key to open or create a
 * protected resource has timed out.
 *
 * @since  TrueCommons 2.2
 * @author Christian Schlichtherle
 */
@ThreadSafe
public class KeyPromptingTimeoutException extends UnknownKeyException {
    private static final long serialVersionUID = 7656348612765052586L;

    public KeyPromptingTimeoutException() {
        super("Key prompting has timed out!");
    }

    public KeyPromptingTimeoutException(@Nullable Throwable cause) {
        super("Key prompting has timed out!");
        super.initCause(cause);
    }
}