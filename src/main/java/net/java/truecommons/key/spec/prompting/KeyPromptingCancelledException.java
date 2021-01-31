/*
 * Copyright (C) 2005-2015 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truecommons.key.spec.prompting;

import net.java.truecommons.key.spec.PersistentUnknownKeyException;

import javax.annotation.Nullable;

/**
 * Thrown to indicate that the retrieval of the key to open or create a
 * protected resource has been cancelled.
 * This is normally caused by user input, for example if the user has closed
 * the prompting dialog.
 *
 * @author  Christian Schlichtherle
 */
public class KeyPromptingCancelledException extends PersistentUnknownKeyException {

    private static final long serialVersionUID = 0;

    public KeyPromptingCancelledException() {
        super("Key prompting has been cancelled!");
    }

    public KeyPromptingCancelledException(@Nullable Throwable cause) {
        super("Key prompting has been cancelled!", cause);
    }
}
