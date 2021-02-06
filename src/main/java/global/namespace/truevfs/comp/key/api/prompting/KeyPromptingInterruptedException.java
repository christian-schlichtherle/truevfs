/*
 * Copyright (C) 2005-2015 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package global.namespace.truevfs.comp.key.api.prompting;

import global.namespace.truevfs.comp.key.api.UnknownKeyException;

import javax.annotation.Nullable;

/**
 * Thrown to indicate that prompting for a key to open or create a
 * protected resource has been interrupted.
 *
 * @author Christian Schlichtherle
 */
public class KeyPromptingInterruptedException extends UnknownKeyException  {

    private static final long serialVersionUID = 0;

    public KeyPromptingInterruptedException() {
        super("Key prompting has been interrupted!");
    }

    public KeyPromptingInterruptedException(@Nullable Throwable cause) {
        super("Key prompting has been interrupted!");
        super.initCause(cause);
    }
}