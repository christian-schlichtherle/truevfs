/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.truezip.key;

import javax.annotation.CheckForNull;
import javax.annotation.concurrent.ThreadSafe;

/**
 * Thrown to indicate that prompting for a key to open or create a
 * protected resource has been interrupted.
 *
 * @author Christian Schlichtherle
 */
@ThreadSafe
public class KeyPromptingInterruptedException extends UnknownKeyException  {
    private static final long serialVersionUID = 7656348607356445644L;

    public KeyPromptingInterruptedException() {
        super("Key prompting has been interrupted!");
    }

    public KeyPromptingInterruptedException(@CheckForNull Throwable cause) {
        super("Key prompting has been interrupted!");
        super.initCause(cause);
    }
}