/*
 * Copyright (C) 2005-2015 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truecommons.key.spec.prompting;

import net.java.truecommons.key.spec.UnknownKeyException;

import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;

/**
 * Thrown to indicate that prompting for a key to open or create a
 * protected resource has been interrupted.
 *
 * @since  TrueCommons 2.2
 * @author Christian Schlichtherle
 */
@ThreadSafe
public class KeyPromptingInterruptedException extends UnknownKeyException  {
    private static final long serialVersionUID = 7656348607356445644L;

    public KeyPromptingInterruptedException() {
        super("Key prompting has been interrupted!");
    }

    public KeyPromptingInterruptedException(@Nullable Throwable cause) {
        super("Key prompting has been interrupted!");
        super.initCause(cause);
    }
}