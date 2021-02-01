/*
 * Copyright (C) 2005-2015 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package global.namespace.truevfs.comp.key.spec.prompting;

import global.namespace.truevfs.comp.key.spec.UnknownKeyException;

import javax.annotation.Nullable;

/**
 * Thrown to indicate that prompting for a key to open or create a
 * protected resource has timed out.
 *
 * @author Christian Schlichtherle
 */
public class KeyPromptingTimeoutException extends UnknownKeyException {

    private static final long serialVersionUID = 0;

    public KeyPromptingTimeoutException() {
        super("Key prompting has timed out!");
    }

    public KeyPromptingTimeoutException(@Nullable Throwable cause) {
        super("Key prompting has timed out!");
        super.initCause(cause);
    }
}